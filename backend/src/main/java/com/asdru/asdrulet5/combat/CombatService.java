package com.asdru.asdrulet5.combat;

import com.asdru.asdrulet5.classdata.ClassDefinitionRegistry;
import com.asdru.asdrulet5.classdata.domain.ClassDefinition;
import com.asdru.asdrulet5.classdata.domain.Stats;
import com.asdru.asdrulet5.classdata.domain.UltimateAbility;
import com.asdru.asdrulet5.combat.domain.*;
import com.asdru.asdrulet5.combat.exception.CombatNotFoundException;
import com.asdru.asdrulet5.combat.web.CombatMapper;
import com.asdru.asdrulet5.combat.web.dto.CombatStateDto;
import com.asdru.asdrulet5.enemydata.EnemyDefinitionRegistry;
import com.asdru.asdrulet5.enemydata.EnemyEncounterRegistry;
import com.asdru.asdrulet5.enemydata.domain.EnemyDefinition;
import com.asdru.asdrulet5.inventory.ItemDefinitionRegistry;
import com.asdru.asdrulet5.inventory.domain.ItemDefinition;
import com.asdru.asdrulet5.inventory.domain.ItemPassive;
import com.asdru.asdrulet5.inventory.domain.Loadout;
import com.asdru.asdrulet5.party.domain.PartyMember;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CombatService {

    /**
     * The dungeon doesn't have distinct floors yet — every encounter table
     * currently sits on {@code EnemyEncounterRegistry}'s floor 1, so this is
     * the only floor ever drawn from. Kept as its own constant (rather than
     * a bare literal at each call site) so swapping this for the party's
     * actual depth, once floors exist, is a one-line change here.
     */
    private static final int CURRENT_FLOOR = 1;

    private final CombatRepository combatRepository;
    private final ClassDefinitionRegistry classDefinitionRegistry;
    private final EnemyDefinitionRegistry enemyDefinitionRegistry;
    private final EnemyEncounterRegistry enemyEncounterRegistry;
    private final ItemDefinitionRegistry itemDefinitionRegistry;
    private final SimpMessagingTemplate messagingTemplate;
    private final ApplicationEventPublisher eventPublisher;
    private final SecureRandom random = new SecureRandom();

    /**
     * isBossFight spawns exactly one {@code EnemyDefinitionRegistry.DEFAULT_ENEMY_ID}
     * (unchanged from before this ever had a notion of multiple enemies);
     * otherwise 2-3 enemies are drawn from {@link EnemyEncounterRegistry}'s
     * current-floor table, which may repeat the same enemy more than once.
     */
    public CombatStateDto startCombat(String code, List<PartyMember> members, List<String> turnOrder, boolean isBossFight) {
        List<Combatant> combatants = new ArrayList<>();
        int leaderCurrentHealth = leaderCurrentHealth(members);
        for (String userId : turnOrder) {
            PartyMember member = members.stream()
                    .filter(candidate -> candidate.userId().equals(userId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Turn order references unknown member " + userId));
            combatants.add(toCombatant(member, leaderCurrentHealth));
        }
        List<Combatant> enemyCombatants = toEnemyCombatants(isBossFight);
        combatants.addAll(enemyCombatants);

        List<String> turnSequence = new ArrayList<>(turnOrder);
        enemyCombatants.forEach(enemy -> turnSequence.add(enemy.combatantId()));

        Combat combat = new Combat(code, combatants, turnSequence);
        combatRepository.save(combat);
        return broadcast(combat);
    }

    public CombatStateDto useAbility(String code, String actorId, String abilityId, String targetId) {
        Combat combat = getOrThrow(code);
        combat.useAbility(actorId, abilityId, targetId);
        publishVictoryIfWon(combat);
        return broadcast(combat);
    }

    public CombatStateDto endTurn(String code, String actorId) {
        Combat combat = getOrThrow(code);
        combat.endTurn(actorId);
        publishVictoryIfWon(combat);
        return broadcast(combat);
    }

    /**
     * Combat.requireInProgress() rejects any call once status is no longer
     * IN_PROGRESS, so the one call whose own action caused the PARTY_WON
     * transition is the only call that will ever observe it here — safe to
     * publish unconditionally on that observation without double-firing.
     */
    private void publishVictoryIfWon(Combat combat) {
        if (combat.status() == CombatStatus.PARTY_WON) {
            eventPublisher.publishEvent(new CombatVictoryEvent(combat.code()));
        }
    }

    public CombatStateDto getState(String code) {
        return CombatMapper.toDto(getOrThrow(code));
    }

    /**
     * The non-enemy combatants from a resolved fight, in their exact ending
     * state (health, remaining active effects) — used by PartyService right
     * after a victory to carry that state back onto each PartyMember, so it
     * persists into whatever room comes next instead of being lost the
     * moment combat ends.
     */
    public List<Combatant> partyCombatantsFor(String code) {
        return getOrThrow(code).combatants().stream().filter(combatant -> !combatant.enemy()).toList();
    }

    /**
     * The party leader's own starting health for this fight — the baseline
     * Mantle of the Usurper compares every other member against, evaluated
     * once here rather than continuously during the fight (see
     * ItemPassive.damagePercentIfHealthierThanLeader's own doc). 0 if
     * there's somehow no leader in the roster.
     */
    private int leaderCurrentHealth(List<PartyMember> members) {
        return members.stream()
                .filter(PartyMember::leader)
                .findFirst()
                .map(this::effectiveStartingHealth)
                .orElse(0);
    }

    /**
     * A member's health entering the fight: whatever carried over, or their full effective max if nothing did.
     */
    private int effectiveStartingHealth(PartyMember member) {
        if (member.currentHealth() != null) {
            return member.currentHealth();
        }
        ClassDefinition definition = classDefinitionRegistry.get(member.characterClass());
        List<ItemPassive> passives = resolvePassives(member.loadout());
        return Math.max(1, definition.stats().maxHealth() + sumBonus(passives, ItemPassive::bonusMaxHealth));
    }

    private Combatant toCombatant(PartyMember member, int leaderCurrentHealth) {
        ClassDefinition definition = classDefinitionRegistry.get(member.characterClass());
        int ultimateChargeThreshold = definition.abilities().stream()
                .filter(UltimateAbility.class::isInstance)
                .map(UltimateAbility.class::cast)
                .findFirst()
                .orElseThrow()
                .chargeThreshold();
        List<ItemPassive> passives = resolvePassives(member.loadout());
        // Evaluated once against this member's own starting health (not the
        // max health this very bonus would produce) to avoid a circular
        // definition — see ItemPassive.bonusMaxHealthPercentIfHealthierThanLeader.
        boolean healthierThanLeader = effectiveStartingHealth(member) > leaderCurrentHealth;
        int baseMaxHealth = definition.stats().maxHealth();
        int maxHealthBonus = sumBonus(passives, ItemPassive::bonusMaxHealth)
                + (healthierThanLeader ? baseMaxHealth * sumBonus(passives, ItemPassive::bonusMaxHealthPercentIfHealthierThanLeader) / 100 : 0);
        // Only the party-leader-relative slice needs to be resolved here —
        // a passive's own flat/dynamic damagePercent contribution is now
        // summed live by Combatant.damagePercentBonus() itself.
        int bonusDamagePercent = healthierThanLeader ? sumBonus(passives, ItemPassive::damagePercentIfHealthierThanLeader) : 0;
        Stats stats = new Stats(
                Math.max(1, baseMaxHealth + maxHealthBonus),
                Math.max(0, definition.stats().defense() + sumBonus(passives, ItemPassive::bonusDefense)),
                Math.max(0, definition.stats().maxStamina() + sumBonus(passives, ItemPassive::bonusMaxStamina)));
        Combatant combatant = new PlayerCombatant(
                member.userId(), member.displayName(), member.characterClass(), stats,
                bonusDamagePercent, ultimateChargeThreshold, definition.abilities(), passives);
        // Both carried over from whatever the member's last room left them
        // with — a wheel/loot roll, or the ending state of their last fight
        // (see PartyService.syncMembersAfterCombat) — so a fresh Combatant
        // always starts from where the party actually left off.
        if (member.currentHealth() != null) {
            combatant.setStartingHealth(member.currentHealth());
        }
        member.pendingEffects().forEach(combatant::addActiveEffect);
        return combatant;
    }

    private List<ItemPassive> resolvePassives(Loadout loadout) {
        return loadout.equippedItemIds().stream()
                .map(itemDefinitionRegistry::get)
                .map(ItemDefinition::passive)
                .toList();
    }

    /**
     * Summed here rather than clamped per-item, so the floor (applied by the
     * caller) only kicks in once against the combined total — a trade-off
     * item's negative bonus could otherwise combine with a low base stat to
     * go to zero or below.
     */
    private int sumBonus(List<ItemPassive> passives, ToIntFunction<ItemPassive> bonus) {
        return passives.stream().mapToInt(bonus).sum();
    }

    /**
     * A boss fight is exactly one {@code DEFAULT_ENEMY_ID}, same as before
     * this ever had a notion of multiple enemies. A regular fight rolls
     * {@link EnemyEncounterRegistry}'s current-floor table, which can repeat
     * the same enemy — when it does, each repeat gets a " 1"/" 2"/... suffix
     * on its display name so e.g. two Cave Rats are distinguishable in the
     * UI (their combatant ids, "enemy-1"/"enemy-2"/..., already keep them
     * distinct to the engine either way).
     */
    private List<Combatant> toEnemyCombatants(boolean isBossFight) {
        List<String> enemyDefinitionIds = isBossFight
                ? List.of(EnemyDefinitionRegistry.DEFAULT_ENEMY_ID)
                : enemyEncounterRegistry.forFloor(CURRENT_FLOOR).roll(random);
        Map<String, Long> occurrences = enemyDefinitionIds.stream()
                .collect(Collectors.groupingBy(id -> id, Collectors.counting()));
        Map<String, Integer> seenSoFar = new HashMap<>();
        List<Combatant> enemies = new ArrayList<>();
        for (int i = 0; i < enemyDefinitionIds.size(); i++) {
            String definitionId = enemyDefinitionIds.get(i);
            EnemyDefinition definition = enemyDefinitionRegistry.get(definitionId);
            String displayName = occurrences.get(definitionId) > 1
                    ? definition.displayName() + " " + seenSoFar.merge(definitionId, 1, Integer::sum)
                    : definition.displayName();
            enemies.add(new EnemyCombatant(
                    "enemy-" + (i + 1), displayName, definitionId, definition.stats(),
                    0, definition.abilities(), definition.passives()));
        }
        return enemies;
    }

    private Combat getOrThrow(String code) {
        return combatRepository.findByCode(code).orElseThrow(() -> new CombatNotFoundException(code));
    }

    private CombatStateDto broadcast(Combat combat) {
        CombatStateDto dto = CombatMapper.toDto(combat);
        messagingTemplate.convertAndSend("/topic/party/" + combat.code() + "/combat", dto);
        return dto;
    }
}
