package com.asdru.asdrulet5.party.web.dto;

public record PendingEffectDto(
        String name,
        String description,
        String icon,
        int remainingTurns
) {
}
