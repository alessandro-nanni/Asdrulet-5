package com.asdru.asdrulet5.combat.web.dto;

import com.asdru.asdrulet5.party.domain.CharacterClass;

import java.util.List;

public record CombatantDto(
        String id,
        String displayName,
        boolean enemy,
        CharacterClass characterClass,
        /** Which EnemyDefinition this enemy combatant was built from (e.g. "cave-rat") — null for party members. Lets the client key a per-species portrait, the same way item ids key item icons. */
        String enemyDefinitionId,
        int maxHealth,
        int currentHealth,
        int maxStamina,
        int currentStamina,
        int defense,
        int ultimateCharge,
        int ultimateChargeThreshold,
        boolean alive,
        List<ActiveEffectDto> activeEffects,
        String attackName,
        String attackDescription,
        String attackEffectSummary
) {
}
