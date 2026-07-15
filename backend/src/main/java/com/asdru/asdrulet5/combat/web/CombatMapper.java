package com.asdru.asdrulet5.combat.web;

import com.asdru.asdrulet5.classdata.domain.Ability;
import com.asdru.asdrulet5.classdata.domain.ActiveEffect;
import com.asdru.asdrulet5.combat.domain.*;
import com.asdru.asdrulet5.combat.web.dto.ActiveEffectDto;
import com.asdru.asdrulet5.combat.web.dto.CombatEventDto;
import com.asdru.asdrulet5.combat.web.dto.CombatStateDto;
import com.asdru.asdrulet5.combat.web.dto.CombatantDto;
import com.asdru.asdrulet5.party.domain.CharacterClass;
import lombok.experimental.UtilityClass;

@UtilityClass
public class CombatMapper {

    public CombatStateDto toDto(Combat combat) {
        return new CombatStateDto(
                combat.code(),
                combat.status(),
                combat.combatants().stream().map(CombatMapper::toDto).toList(),
                combat.currentTurnCombatantId(),
                combat.lastEvents().stream().map(CombatMapper::toDto).toList()
        );
    }

    private CombatantDto toDto(Combatant combatant) {
        CharacterClass characterClass = combatant instanceof PlayerCombatant player ? player.characterClass() : null;
        String enemyDefinitionId = combatant instanceof EnemyCombatant enemy ? enemy.id() : null;
        // Only enemies have ever surfaced "attack" info on the DTO — a
        // player's own moves are already known client-side via their class
        // definition, so this mirrors that: the enemy's first ability
        // (see Combat.resolveEnemyTurn) stands in for what used to be a
        // dedicated attackName/attackDescription/attackEffectSummary on
        // Combatant itself.
        Ability primaryAbility = combatant.enemy() && !combatant.abilities().isEmpty()
                ? combatant.abilities().getFirst()
                : null;
        return new CombatantDto(
                combatant.combatantId(),
                combatant.displayName(),
                combatant.enemy(),
                characterClass,
                enemyDefinitionId,
                combatant.maxHealth(),
                combatant.currentHealth(),
                combatant.maxStamina(),
                combatant.currentStamina(),
                combatant.stats().defense(),
                combatant.ultimateCharge(),
                combatant.ultimateChargeThreshold(),
                combatant.alive(),
                combatant.activeEffects().stream().map(CombatMapper::toDto).toList(),
                primaryAbility != null ? primaryAbility.name() : null,
                primaryAbility != null ? primaryAbility.description() : null,
                primaryAbility != null ? primaryAbility.effectSummary() : null
        );
    }

    private ActiveEffectDto toDto(ActiveEffect activeEffect) {
        return new ActiveEffectDto(
                activeEffect.name(), activeEffect.description(), activeEffect.icon(), activeEffect.remainingTurns());
    }

    private CombatEventDto toDto(CombatEvent event) {
        return new CombatEventDto(event.targetId(), event.kind(), event.amount(), event.critical());
    }
}
