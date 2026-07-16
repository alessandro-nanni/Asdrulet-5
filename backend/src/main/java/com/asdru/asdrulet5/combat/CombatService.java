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
import com.asdru.asdrulet5.party.domain.Party;
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
    public CombatStateDto startCombat(Party party, List<PartyMember> members, List<String> turnOrder, boolean isBossFight) {
        List<Combatant> combatants = new ArrayList<>();
        List<PreparedPlayerCombatant> playerCombatants = new ArrayList<>();
        for (String userId : turnOrder) {
            PartyMember member = members.stream()
                    .filter(candidate -> candidate.userId().equals(userId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Turn order references unknown member " + userId));
            PreparedPlayerCombatant prepared = toCombatant(member, party);
            playerCombatants.add(prepared);
            combatants.add(prepared.combatant());
        }
        resolveLeaderRelativeMaxHealthBonuses(playerCombatants);

        List<Combatant> enemyCombatants = toEnemyCombatants(isBossFight);
        combatants.addAll(enemyCombatants);

        List<String> turnSequence = new ArrayList<>(turnOrder);
        enemyCombatants.forEach(enemy -> turnSequence.add(enemy.combatantId()));

        Combat combat = new Combat(party.code(), combatants, turnSequence);
        combatRepository.save(combat);
        return broadcast(combat);
    }

    /**
     * Wires every player combatant's roster in early — before the enemies
     * even exist, let alone the {@link Combat} they'll all eventually share
     * ({@link Combat}'s own constructor will attach the complete roster
     * again once it's built, superseding this one) — so each one's own
     * {@code wearer.healthierThanLeader()} can already see every other
     * member's starting health, then applies whatever leader-relative max
     * health bonus that unlocks (see Mantle of the Usurper). A damage-percent
     * equivalent needs no such step: it's resolved live, per hit, straight
     * off {@link ItemPassive#damagePercentBonus} once the fight is already
     * running — only max health is stuck being a fight-start-only
     * resolution, since it's baked into Stats when the Combatant is built.
     */
    private void resolveLeaderRelativeMaxHealthBonuses(List<PreparedPlayerCombatant> playerCombatants) {
        List<Combatant> roster = playerCombatants.stream().<Combatant>map(PreparedPlayerCombatant::combatant).toList();
        playerCombatants.forEach(prepared -> prepared.combatant().attachRoster(roster));
        for (PreparedPlayerCombatant prepared : playerCombatants) {
            PlayerCombatant combatant = prepared.combatant();
            int percent = prepared.passives().stream().mapToInt(passive -> passive.bonusMaxHealthPercent(combatant)).sum();
            combatant.increaseMaxHealth(combatant.maxHealth() * percent / 100);
        }
    }

    /**
     * A player combatant alongside the resolved {@link ItemPassive}s that
     * built it — kept together only long enough for
     * {@link #resolveLeaderRelativeMaxHealthBonuses} to re-consult those same
     * passives without having to re-derive them from the Combatant's own,
     * type-erased {@code CombatantPassive} list.
     */
    private record PreparedPlayerCombatant(PlayerCombatant combatant, List<ItemPassive> passives) {
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
     * Builds a fresh PlayerCombatant with its flat item bonuses only — any
     * leader-relative bonus (see Mantle of the Usurper) is deliberately left
     * unresolved here and applied afterward, once every player combatant in
     * this fight exists and has its starting health set, by
     * {@link #resolveLeaderRelativeMaxHealthBonuses}.
     */
    private PreparedPlayerCombatant toCombatant(PartyMember member, Party party) {
        ClassDefinition definition = classDefinitionRegistry.get(member.characterClass());
        int ultimateChargeThreshold = definition.abilities().stream()
                .filter(UltimateAbility.class::isInstance)
                .map(UltimateAbility.class::cast)
                .findFirst()
                .orElseThrow()
                .chargeThreshold();
        List<ItemPassive> passives = resolvePassives(member.loadout());
        int baseMaxHealth = definition.stats().maxHealth();
        Stats stats = new Stats(
                Math.max(1, baseMaxHealth + sumBonus(passives, ItemPassive::bonusMaxHealth)),
                Math.max(0, definition.stats().defense() + sumBonus(passives, ItemPassive::bonusDefense)),
                Math.max(0, definition.stats().maxStamina() + sumBonus(passives, ItemPassive::bonusMaxStamina)));
        PlayerCombatant combatant = new PlayerCombatant(
                member.userId(), member.displayName(), member.characterClass(), stats,
                ultimateChargeThreshold, definition.abilities(), passives, party);
        // Both carried over from whatever the member's last room left them
        // with — a wheel/loot roll, or the ending state of their last fight
        // (see PartyService.syncMembersAfterCombat) — so a fresh Combatant
        // always starts from where the party actually left off. Set before
        // any leader-relative max health bonus, deliberately: that bonus
        // still needs to compare against this member's own pre-bonus
        // starting health, exactly as if it were never carried over.
        if (member.currentHealth() != null) {
            combatant.setStartingHealth(member.currentHealth());
        }
        member.pendingEffects().forEach(combatant::addActiveEffect);
        return new PreparedPlayerCombatant(combatant, passives);
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
