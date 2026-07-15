package com.asdru.asdrulet5.enemydata.domain;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class EnemyPoolTest {

    private static EnemyPoolEntry entry(String enemyId, int weight) {
        return new EnemyPoolEntry(enemyId, weight);
    }

    @Test
    void pickRandomCanReturnTheSameEntryMoreThanOnce() {
        // A single-entry candidate list forced to draw 3 times can only ever
        // repeat that same entry — proves this is sampling *with* replacement.
        List<EnemyPoolEntry> pool = List.of(entry("only", 10));

        List<EnemyPoolEntry> picked = EnemyPool.pickRandom(pool, 3, new Random(1));

        assertThat(picked).hasSize(3);
        assertThat(picked).allMatch(candidate -> candidate.enemyId().equals("only"));
    }

    @Test
    void pickRandomReturnsEmptyForAnEmptyPool() {
        assertThat(EnemyPool.pickRandom(List.of(), 3, new Random(1))).isEmpty();
    }

    @Test
    void pickRandomReturnsExactlyCountEntries() {
        List<EnemyPoolEntry> pool = List.of(entry("a", 10), entry("b", 10));

        assertThat(EnemyPool.pickRandom(pool, 5, new Random(1))).hasSize(5);
    }

    /**
     * Not a statistical proof, just a sanity check that weight actually
     * biases the draw: an entry weighted 99-to-1 against another should win
     * the overwhelming majority of a large number of single-item draws.
     */
    @Test
    void higherWeightIsDrawnMoreOften() {
        List<EnemyPoolEntry> pool = List.of(entry("common", 99), entry("rare", 1));

        List<EnemyPoolEntry> picked = EnemyPool.pickRandom(pool, 2000, new Random(7));

        long commonCount = picked.stream().filter(candidate -> candidate.enemyId().equals("common")).count();
        assertThat((double) commonCount / picked.size()).isGreaterThan(0.9);
    }
}
