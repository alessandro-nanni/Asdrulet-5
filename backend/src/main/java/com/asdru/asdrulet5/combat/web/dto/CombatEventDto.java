package com.asdru.asdrulet5.combat.web.dto;

import com.asdru.asdrulet5.combat.domain.CombatEvent;

public record CombatEventDto(
        String targetId,
        CombatEvent.Kind kind,
        int amount,
        boolean critical
) {
}
