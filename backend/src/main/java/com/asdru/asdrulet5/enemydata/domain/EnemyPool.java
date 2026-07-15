package com.asdru.asdrulet5.enemydata.domain;

import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Weighted sampling, <b>with</b> replacement, over a list of enemy pool
 * entries — deliberately unlike
 * {@code com.asdru.asdrulet5.inventory.domain.LootPool}'s without-replacement
 * draw: a loot roll shouldn't hand back two of the same item, but an
 * encounter of "2 Cave Rats and a Bandit Thug" is a completely normal
 * result, so the same entry can be picked more than once in a single roll.
 */
@UtilityClass
public class EnemyPool {

    /** Picks {@code count} entries independently (each its own fresh weighted roll), returning an empty list if candidates is empty. */
    public List<EnemyPoolEntry> pickRandom(List<EnemyPoolEntry> candidates, int count, Random random) {
        if (candidates.isEmpty()) {
            return List.of();
        }
        List<EnemyPoolEntry> picked = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            picked.add(pickOne(candidates, random));
        }
        return picked;
    }

    private EnemyPoolEntry pickOne(List<EnemyPoolEntry> candidates, Random random) {
        int totalWeight = candidates.stream().mapToInt(EnemyPoolEntry::weight).sum();
        int roll = random.nextInt(totalWeight);
        int cumulative = 0;
        for (EnemyPoolEntry candidate : candidates) {
            cumulative += candidate.weight();
            if (roll < cumulative) {
                return candidate;
            }
        }
        // Unreachable: roll is always < totalWeight, and the loop's final
        // cumulative equals totalWeight exactly, so some iteration always returns first.
        throw new IllegalStateException("Failed to pick a weighted entry — this should be unreachable");
    }
}
