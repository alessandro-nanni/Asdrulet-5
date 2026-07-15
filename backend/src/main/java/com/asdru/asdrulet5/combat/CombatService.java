package com.asdru.asdrulet5.combat;

import com.asdru.asdrulet5.classdata.ClassDefinitionRegistry;
import com.asdru.asdrulet5.classdata.domain.ClassDefinition;
import com.asdru.asdrulet5.classdata.domain.UltimateAbility;
import com.asdru.asdrulet5.combat.domain.Combat;
import com.asdru.asdrulet5.combat.domain.CombatStatus;
import com.asdru.asdrulet5.combat.domain.Combatant;
import com.asdru.asdrulet5.combat.exception.CombatNotFoundException;
import com.asdru.asdrulet5.combat.web.CombatMapper;
import com.asdru.asdrulet5.combat.web.dto.CombatStateDto;
import com.asdru.asdrulet5.enemydata.EnemyDefinitionRegistry;
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

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

@Service
@RequiredArgsConstructor
public class CombatService {

    private static final String ENEMY_ID = "enemy-1";

    private final CombatRepository combatRepository;
    private final ClassDefinitionRegistry classDefinitionRegistry;
    private final EnemyDefinitionRegistry enemyDefinitionRegistry;
    private final ItemDefinitionRegistry itemDefinitionRegistry;
    private final SimpMessagingTemplate messagingTemplate;
    private final ApplicationEventPublisher eventPublisher;

    public CombatStateDto startCombat(String code, List<PartyMember> members, List<String> turnOrder) {
        List<Combatant> combatants = new ArrayList<>();
        for (String userId : turnOrder) {
            PartyMember member = members.stream()
                    .filter(candidate -> candidate.userId().equals(userId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Turn order references unknown member " + userId));
            combatants.add(toCombatant(member));
        }
        combatants.add(toEnemyCombatant());

        List<String> turnSequence = new ArrayList<>(turnOrder);
        turnSequence.add(ENEMY_ID);

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

    private Combatant toCombatant(PartyMember member) {
        ClassDefinition definition = classDefinitionRegistry.get(member.characterClass());
        int ultimateChargeThreshold = definition.abilities().stream()
                .filter(UltimateAbility.class::isInstance)
                .map(UltimateAbility.class::cast)
                .findFirst()
                .orElseThrow()
                .chargeThreshold();
        List<ItemPassive> passives = resolvePassives(member.loadout());
        Combatant combatant = new Combatant(
                member.userId(), member.displayName(), false, member.characterClass(),
                Math.max(1, definition.stats().maxHealth() + sumBonus(passives, ItemPassive::bonusMaxHealth)),
                Math.max(0, definition.stats().maxStamina() + sumBonus(passives, ItemPassive::bonusMaxStamina)),
                Math.max(0, definition.stats().defense() + sumBonus(passives, ItemPassive::bonusDefense)),
                sumBonus(passives, ItemPassive::damagePercent),
                ultimateChargeThreshold, definition.abilities(), null, null, null, null, passives);
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

    private Combatant toEnemyCombatant() {
        EnemyDefinition definition = enemyDefinitionRegistry.get(EnemyDefinitionRegistry.DEFAULT_ENEMY_ID);
        return new Combatant(
                ENEMY_ID, definition.displayName(), true, null,
                definition.stats().maxHealth(), definition.stats().maxStamina(), definition.stats().defense(),
                0, 0, List.of(), definition.attackName(), definition.attackDescription(),
                definition.attackEffectSummary(), definition.attackEffect(), List.of());
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
