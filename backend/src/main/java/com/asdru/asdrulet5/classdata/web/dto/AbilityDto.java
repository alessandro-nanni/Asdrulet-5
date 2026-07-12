package com.asdru.asdrulet5.classdata.web.dto;

import com.asdru.asdrulet5.classdata.domain.TargetType;

/**
 * Flat wire shape for both ability kinds. Exactly one of staminaCost /
 * chargeThreshold is populated, matching type. effectSummary is an
 * author-written mechanical summary (e.g. "22 damage") — the actual effect
 * logic is arbitrary code and isn't represented on the wire.
 */
public record AbilityDto(
        String id,
        String name,
        String description,
        String effectSummary,
        TargetType targetType,
        AbilityKind type,
        Integer staminaCost,
        Integer chargeThreshold
) {
    public enum AbilityKind {BASIC, ULTIMATE}
}
