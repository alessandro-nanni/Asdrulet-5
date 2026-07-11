package com.asdru.asdrulet5.enemydata.domain;

import com.asdru.asdrulet5.classdata.domain.AbilityEffect;
import com.asdru.asdrulet5.classdata.domain.DamageEffect;
import com.asdru.asdrulet5.classdata.domain.Stats;

import java.util.Objects;

public record EnemyDefinition(
        String id,
        String displayName,
        Stats stats,
        String attackName,
        String attackDescription,
        AbilityEffect attackEffect
) {
    public EnemyDefinition {
        requireNonBlank(id, "id");
        requireNonBlank(displayName, "displayName");
        Objects.requireNonNull(stats, "stats");
        requireNonBlank(attackName, "attackName");
        requireNonBlank(attackDescription, "attackDescription");
        Objects.requireNonNull(attackEffect, "attackEffect");
        if (!(attackEffect instanceof DamageEffect)) {
            throw new IllegalArgumentException("attackEffect must be a DamageEffect, was " + attackEffect.getClass());
        }
    }

    private static void requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }
}
