package com.asdru.asdrulet5.inventory.domain;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class LootTableTest {

    private static LootTableEntry entry(String itemId, int weight) {
        return new LootTableEntry(itemId, weight);
    }

    @Test
    void rollDrawsExactlyTheFixedAmount() {
        LootTable table = new LootTable(
                List.of(entry("a", 10), entry("b", 10), entry("c", 10)),
                LootAmount.fixed(2));

        assertThat(table.roll(new Random(42))).hasSize(2);
    }

    @Test
    void rollDrawsWithinTheRangedAmount() {
        LootTable table = new LootTable(
                List.of(entry("a", 10), entry("b", 10), entry("c", 10)),
                new LootAmount(1, 3));
        Random random = new Random(42);

        for (int i = 0; i < 50; i++) {
            assertThat(table.roll(random).size()).isBetween(1, 3);
        }
    }

    @Test
    void rollExcludingDropsExcludedItemsFirst() {
        LootTable table = new LootTable(
                List.of(entry("a", 10), entry("b", 10)),
                LootAmount.fixed(1));

        List<String> result = table.rollExcluding(Set.of("a"), new Random(42));

        assertThat(result).containsExactly("b");
    }

    @Test
    void rollExcludingFallsBackToTheFullTableIfEverythingIsExcluded() {
        LootTable table = new LootTable(
                List.of(entry("a", 10), entry("b", 10)),
                LootAmount.fixed(1));

        List<String> result = table.rollExcluding(Set.of("a", "b"), new Random(42));

        assertThat(result).hasSize(1);
        assertThat(result.getFirst()).isIn("a", "b");
    }

    @Test
    void rollOnAnEmptyTableReturnsEmpty() {
        LootTable table = new LootTable(List.of(), LootAmount.fixed(8));

        assertThat(table.roll(new Random(42))).isEmpty();
    }

    @Test
    void neverReturnsDuplicateItemsInOneRoll() {
        LootTable table = new LootTable(
                List.of(entry("a", 10), entry("b", 10), entry("c", 10)),
                LootAmount.fixed(3));

        assertThat(table.roll(new Random(42))).containsExactlyInAnyOrder("a", "b", "c");
    }
}
