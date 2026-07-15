package com.asdru.asdrulet5.inventory.domain;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class LootPoolTest {

    private static LootTableEntry entry(String itemId, int weight) {
        return new LootTableEntry(itemId, weight);
    }

    @Test
    void pickRandomNeverReturnsTheSameEntryTwice() {
        List<LootTableEntry> pool = List.of(entry("a", 10), entry("b", 10), entry("c", 10));

        List<LootTableEntry> picked = LootPool.pickRandom(pool, 3, new Random(42));

        assertThat(picked).extracting(LootTableEntry::itemId).containsExactlyInAnyOrder("a", "b", "c");
    }

    @Test
    void pickRandomClampsCountToHowManyCandidatesExist() {
        List<LootTableEntry> pool = List.of(entry("a", 10), entry("b", 10));

        List<LootTableEntry> picked = LootPool.pickRandom(pool, 5, new Random(42));

        assertThat(picked).hasSize(2);
    }

    @Test
    void pickRandomReturnsEmptyForAnEmptyPool() {
        assertThat(LootPool.pickRandom(List.of(), 3, new Random(42))).isEmpty();
    }

    /**
     * Not a statistical proof, just a sanity check that weight actually
     * biases the draw: an entry weighted 99-to-1 against another should win
     * the overwhelming majority of a large number of single-item draws.
     */
    @Test
    void higherWeightIsDrawnMoreOften() {
        List<LootTableEntry> pool = List.of(entry("common", 99), entry("rare", 1));
        Random random = new Random(7);

        long commonCount = 0;
        int trials = 2000;
        for (int i = 0; i < trials; i++) {
            if (LootPool.pickOneRandom(pool, random).itemId().equals("common")) {
                commonCount++;
            }
        }

        assertThat((double) commonCount / trials).isGreaterThan(0.9);
    }

    @Test
    void pickOneRandomAlwaysReturnsTheOnlyCandidateWhenThereIsJustOne() {
        List<LootTableEntry> pool = List.of(entry("only", 1));

        assertThat(LootPool.pickOneRandom(pool, new Random(1)).itemId()).isEqualTo("only");
    }
}
