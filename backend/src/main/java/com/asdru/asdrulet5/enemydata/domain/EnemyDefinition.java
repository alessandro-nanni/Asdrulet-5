package com.asdru.asdrulet5.enemydata.domain;

import com.asdru.asdrulet5.classdata.domain.AbilityEffect;
import com.asdru.asdrulet5.classdata.domain.Stats;

import java.util.Objects;

public record EnemyDefinition(
        String id,
        String displayName,
        Stats stats,
        String attackName,
        String attackDescription,
        String attackEffectSummary,
        AbilityEffect attackEffect
) {
    public EnemyDefinition {
        requireNonBlank(id, "id");
        requireNonBlank(displayName, "displayName");
        Objects.requireNonNull(stats, "stats");
        requireNonBlank(attackName, "attackName");
        requireNonBlank(attackDescription, "attackDescription");
        requireNonBlank(attackEffectSummary, "attackEffectSummary");
        Objects.requireNonNull(attackEffect, "attackEffect");
    }

    private static void requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }
}
