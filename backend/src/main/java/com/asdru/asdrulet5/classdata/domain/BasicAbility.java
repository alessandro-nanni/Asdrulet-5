package com.asdru.asdrulet5.classdata.domain;

import java.util.Objects;

import static com.asdru.asdrulet5.classdata.domain.Preconditions.requireNonBlank;

public record BasicAbility(
        String id,
        String name,
        String description,
        TargetType targetType,
        int staminaCost,
        AbilityEffect effect
) implements Ability {
    public BasicAbility {
        requireNonBlank(id, "id");
        requireNonBlank(name, "name");
        requireNonBlank(description, "description");
        Objects.requireNonNull(targetType, "targetType");
        Objects.requireNonNull(effect, "effect");
        if (staminaCost < 0) {
            throw new IllegalArgumentException("staminaCost must not be negative");
        }
    }
}
