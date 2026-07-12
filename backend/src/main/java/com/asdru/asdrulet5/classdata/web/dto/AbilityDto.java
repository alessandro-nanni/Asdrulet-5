package com.asdru.asdrulet5.classdata.web.dto;

import com.asdru.asdrulet5.classdata.domain.TargetType;

/**
 * Flat wire shape for both ability kinds. Exactly one of staminaCost /
 * chargeThreshold is populated, matching type.
 */
public record AbilityDto(
        String id,
        String name,
        String description,
        TargetType targetType,
        AbilityKind type,
        Integer staminaCost,
        Integer chargeThreshold,
        EffectDto effect
) {
    public enum AbilityKind {BASIC, ULTIMATE}
}
