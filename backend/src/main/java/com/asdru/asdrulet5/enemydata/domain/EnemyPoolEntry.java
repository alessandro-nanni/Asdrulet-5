package com.asdru.asdrulet5.enemydata.domain;

/**
 * One enemy's membership in an {@link EncounterTable} — separate from
 * {@link EnemyDefinition} itself for the same reason
 * {@code com.asdru.asdrulet5.inventory.domain.LootTableEntry} is kept apart
 * from item definitions: how likely (and how often alongside others) an
 * enemy shows up in an encounter is a drop-table concern, not a property of
 * the enemy.
 */
public record EnemyPoolEntry(String enemyId, int weight) {
    public EnemyPoolEntry {
        if (enemyId == null || enemyId.isBlank()) {
            throw new IllegalArgumentException("enemyId must not be blank");
        }
        if (weight <= 0) {
            throw new IllegalArgumentException("weight must be positive");
        }
    }
}
