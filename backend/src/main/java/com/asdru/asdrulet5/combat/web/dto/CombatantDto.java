package com.asdru.asdrulet5.combat.web.dto;

import com.asdru.asdrulet5.party.domain.CharacterClass;

import java.util.List;

public record CombatantDto(
        String id,
        String displayName,
        boolean enemy,
        CharacterClass characterClass,
        int maxHealth,
        int currentHealth,
        int maxStamina,
        int currentStamina,
        int ultimateCharge,
        int ultimateChargeThreshold,
        boolean alive,
        List<ActiveEffectDto> activeEffects
) {
}
