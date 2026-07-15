package com.asdru.asdrulet5.enemydata.domain;

import com.asdru.asdrulet5.classdata.domain.Ability;
import com.asdru.asdrulet5.classdata.domain.Stats;

import java.util.List;
import java.util.Objects;

/**
 * {@code abilities} reuses the exact same {@link Ability} shape a
 * ClassDefinition's moves do — an enemy attacking is mechanically no
 * different from a player using a basic ability, just always the first one
 * in the list for now (see {@code Combat.resolveEnemyTurn}). Every enemy
 * currently declares exactly one, but the list shape means a boss with a
 * real move set doesn't need a different representation later.
 */
public record EnemyDefinition(
        String id,
        String displayName,
        Stats stats,
        List<Ability> abilities,
        List<EnemyPassive> passives
) {
    public EnemyDefinition {
        requireNonBlank(id, "id");
        requireNonBlank(displayName, "displayName");
        Objects.requireNonNull(stats, "stats");
        if (abilities == null || abilities.isEmpty()) {
            throw new IllegalArgumentException("abilities must not be empty");
        }
        abilities = List.copyOf(abilities);
        passives = List.copyOf(passives);
    }

    private static void requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }
}
