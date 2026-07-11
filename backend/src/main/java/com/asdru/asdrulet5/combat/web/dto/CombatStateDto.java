package com.asdru.asdrulet5.combat.web.dto;

import com.asdru.asdrulet5.combat.domain.CombatStatus;

import java.util.List;

public record CombatStateDto(
        String code,
        CombatStatus status,
        List<CombatantDto> combatants,
        String currentTurnCombatantId
) {
}
