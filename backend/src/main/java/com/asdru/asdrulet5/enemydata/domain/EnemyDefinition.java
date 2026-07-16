package com.asdru.asdrulet5.enemydata.domain;

import com.asdru.asdrulet5.classdata.domain.Ability;
import com.asdru.asdrulet5.classdata.domain.Stats;

import java.util.List;
import java.util.Objects;

/**
 * {@code abilities} reuses the exact same {@link Ability} shape a
 * ClassDefinition's moves do — an enemy attacking is mechanically no
 * different from a player using a basic ability. Which one an enemy picks
 * on its turn is decided by {@code Combat.chooseEnemyAbility}: whichever
 * comes first in this list that it can currently afford, so authors should
 * order a definition's own abilities strongest/most expensive first and a
 * guaranteed-affordable fallback last (see {@code EnemyDefinitionRegistry}).
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
