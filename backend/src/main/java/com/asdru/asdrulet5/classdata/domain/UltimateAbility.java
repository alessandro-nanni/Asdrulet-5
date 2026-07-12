package com.asdru.asdrulet5.classdata.domain;


import java.util.Objects;

/**
 * chargeThreshold is how much accumulated damage-charge is needed to unleash
 * this ability; charge resets to 0 once used. Costs no stamina.
 */
public record UltimateAbility(
        String id,
        String name,
        String description,
        String effectSummary,
        TargetType targetType,
        int chargeThreshold,
        AbilityEffect effect
) implements Ability {
    public UltimateAbility {
        Preconditions.requireNonBlank(id, "id");
        Preconditions.requireNonBlank(name, "name");
        Preconditions.requireNonBlank(description, "description");
        Preconditions.requireNonBlank(effectSummary, "effectSummary");
        Objects.requireNonNull(targetType, "targetType");
        Objects.requireNonNull(effect, "effect");
        if (chargeThreshold < 0) {
            throw new IllegalArgumentException("chargeThreshold must not be negative");
        }
    }
}
