package com.asdru.asdrulet5.combat.web;

import com.asdru.asdrulet5.classdata.web.dto.EffectDto;
import com.asdru.asdrulet5.combat.domain.ActiveEffect;
import com.asdru.asdrulet5.combat.domain.Combat;
import com.asdru.asdrulet5.combat.domain.Combatant;
import com.asdru.asdrulet5.combat.web.dto.ActiveEffectDto;
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
                combat.currentTurnCombatantId()
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
                combatant.activeEffects().stream().map(CombatMapper::toDto).toList()
        );
    }

    private ActiveEffectDto toDto(ActiveEffect activeEffect) {
        EffectDto.Kind kind = switch (activeEffect.kind()) {
            case DEFENSE -> EffectDto.Kind.BUFF_DEFENSE;
            case DAMAGE -> EffectDto.Kind.BUFF_DAMAGE;
        };
        return new ActiveEffectDto(kind, activeEffect.power(), activeEffect.remainingTurns());
    }
}
