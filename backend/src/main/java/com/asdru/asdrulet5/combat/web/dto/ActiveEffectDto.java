package com.asdru.asdrulet5.combat.web.dto;

public record ActiveEffectDto(
        String name,
        String description,
        String icon,
        int remainingTurns
) {
}
