package com.asdru.asdrulet5.party.domain;

import java.util.List;

/**
 * What one party member found opening a LOOT room's chest — coins, one or
 * more items, or both (never neither; see
 * {@link com.asdru.asdrulet5.party.PartyService}'s roll logic). itemIds is
 * empty when the roll didn't include an item; a chest can hand back more
 * than one (see the chest LootTable's own LootAmount).
 */
public record LootResult(int coins, List<String> itemIds) {

    public LootResult {
        if (coins < 0) {
            throw new IllegalArgumentException("coins must not be negative");
        }
        itemIds = List.copyOf(itemIds);
        if (coins == 0 && itemIds.isEmpty()) {
            throw new IllegalArgumentException("a LootResult must have coins, an item, or both");
        }
    }
}
