package com.asdru.asdrulet5.combat.web;

import com.asdru.asdrulet5.classdata.domain.ActiveEffect;
import com.asdru.asdrulet5.combat.domain.Combat;
import com.asdru.asdrulet5.combat.domain.CombatEvent;
import com.asdru.asdrulet5.combat.domain.Combatant;
import com.asdru.asdrulet5.combat.web.dto.ActiveEffectDto;
import com.asdru.asdrulet5.combat.web.dto.CombatEventDto;
import com.asdru.asdrulet5.combat.web.dto.CombatStateDto;
import com.asdru.asdrulet5.combat.web.dto.CombatantDto;
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
        return new CombatantDto(
                combatant.id(),
                combatant.displayName(),
                combatant.enemy(),
                combatant.characterClass(),
                combatant.maxHealth(),
                combatant.currentHealth(),
                combatant.maxStamina(),
                combatant.currentStamina(),
                combatant.ultimateCharge(),
                combatant.ultimateChargeThreshold(),
                combatant.alive(),
                combatant.activeEffects().stream().map(CombatMapper::toDto).toList(),
                combatant.attackName(),
                combatant.attackDescription(),
                combatant.attackEffectSummary()
        );
    }

    private ActiveEffectDto toDto(ActiveEffect activeEffect) {
        return new ActiveEffectDto(
                activeEffect.name(), activeEffect.description(), activeEffect.icon(), activeEffect.remainingTurns());
    }

    private CombatEventDto toDto(CombatEvent event) {
        return new CombatEventDto(event.targetId(), event.kind(), event.amount());
    }
}
