package com.asdru.asdrulet5.inventory.domain;

import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * A named, weighted pool of items plus how many distinct items a single roll
 * draws (see {@link LootAmount}) — e.g. a mystery wheel table always draws
 * 1, a chest table draws a random 1-3, a merchant table draws a fixed 8.
 * Each of those is its own independent LootTable (see
 * {@code LootTableRegistry}), even when they happen to share entries today —
 * nothing stops them diverging later.
 */
public record LootTable(List<LootTableEntry> entries, LootAmount amount) {

    public LootTable {
        entries = List.copyOf(entries);
    }

    /**
     * Rolls this table once: draws {@link #amount}'s count of distinct entries, weighted, and returns their item ids.
     */
    public List<String> roll(Random random) {
        return rollExcluding(Set.of(), random);
    }

    /**
     * Same as {@link #roll(Random)}, but first drops any entry whose item id
     * is in {@code excludedItemIds} — e.g. items already in play somewhere
     * in the party, so a roll feels like discovering something new. Falls
     * back to the table's full entry list if excluding those leaves nothing
     * to draw from, rather than under-delivering.
     */
    public List<String> rollExcluding(Set<String> excludedItemIds, Random random) {
        List<LootTableEntry> eligible = entries.stream()
                .filter(entry -> !excludedItemIds.contains(entry.itemId()))
                .toList();
        List<LootTableEntry> pool = eligible.isEmpty() ? entries : eligible;
        int count = amount.roll(random);
        return LootPool.pickRandom(pool, count, random).stream().map(LootTableEntry::itemId).toList();
    }
}
