package com.asdru.asdrulet5.enemydata.domain;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class EncounterTableTest {

    private static EnemyPoolEntry entry(String enemyId, int weight) {
        return new EnemyPoolEntry(enemyId, weight);
    }

    @Test
    void rollDrawsExactlyTheFixedSize() {
        EncounterTable table = new EncounterTable(
                List.of(entry("a", 10), entry("b", 10)),
                EncounterSize.fixed(1));

        assertThat(table.roll(new Random(1))).hasSize(1);
    }

    @Test
    void rollDrawsWithinTheRangedSize() {
        EncounterTable table = new EncounterTable(
                List.of(entry("a", 10), entry("b", 10)),
                new EncounterSize(2, 3));
        Random random = new Random(1);

        for (int i = 0; i < 50; i++) {
            assertThat(table.roll(random).size()).isBetween(2, 3);
        }
    }

    @Test
    void rollCanRepeatTheSameEnemy() {
        EncounterTable table = new EncounterTable(List.of(entry("only", 10)), EncounterSize.fixed(3));

        assertThat(table.roll(new Random(1))).containsExactly("only", "only", "only");
    }

    @Test
    void rollOnAnEmptyTableReturnsEmpty() {
        EncounterTable table = new EncounterTable(List.of(), EncounterSize.fixed(3));

        assertThat(table.roll(new Random(1))).isEmpty();
    }
}
