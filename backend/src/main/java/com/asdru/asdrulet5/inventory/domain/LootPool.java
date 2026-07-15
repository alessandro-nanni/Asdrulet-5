package com.asdru.asdrulet5.inventory.domain;

import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Weighted, without-replacement sampling over a list of loot table entries —
 * the low-level draw {@link LootTable} itself is built on. Entries with a
 * higher {@link LootTableEntry#weight()} are proportionally more likely to
 * be picked.
 */
@UtilityClass
public class LootPool {

    /**
     * Picks up to {@code count} distinct entries from candidates, weighted
     * by {@link LootTableEntry#weight()} — each pick removes its entry from
     * contention, so the same item never comes back twice. count is clamped
     * to however many candidates exist; this returns fewer than count only
     * when candidates itself is smaller.
     */
    public List<LootTableEntry> pickRandom(List<LootTableEntry> candidates, int count, Random random) {
        List<LootTableEntry> remaining = new ArrayList<>(candidates);
        List<LootTableEntry> picked = new ArrayList<>();
        int toPick = Math.min(count, remaining.size());
        for (int i = 0; i < toPick; i++) {
            picked.add(pickOne(remaining, random));
        }
        return picked;
    }

    /**
     * Same as {@link #pickRandom(List, int, Random)} with count 1 — throws if candidates is empty.
     */
    public LootTableEntry pickOneRandom(List<LootTableEntry> candidates, Random random) {
        return pickRandom(candidates, 1, random).get(0);
    }

    private LootTableEntry pickOne(List<LootTableEntry> remaining, Random random) {
        int totalWeight = remaining.stream().mapToInt(LootTableEntry::weight).sum();
        int roll = random.nextInt(totalWeight);
        int cumulative = 0;
        for (int i = 0; i < remaining.size(); i++) {
            cumulative += remaining.get(i).weight();
            if (roll < cumulative) {
                return remaining.remove(i);
            }
        }
        // Unreachable: roll is always < totalWeight, and the loop's final
        // cumulative equals totalWeight exactly, so some iteration always returns first.
        throw new IllegalStateException("Failed to pick a weighted entry — this should be unreachable");
    }
}
