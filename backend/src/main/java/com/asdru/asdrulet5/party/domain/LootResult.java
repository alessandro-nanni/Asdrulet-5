package com.asdru.asdrulet5.party.domain;

/**
 * What one party member found opening a LOOT room's chest — coins, an item,
 * or both (never neither; see {@link com.asdru.asdrulet5.party.PartyService}'s
 * roll logic). itemId is null when the roll didn't include an item.
 */
public record LootResult(int coins, String itemId) {

    public LootResult {
        if (coins < 0) {
            throw new IllegalArgumentException("coins must not be negative");
        }
        if (coins == 0 && itemId == null) {
            throw new IllegalArgumentException("a LootResult must have coins, an item, or both");
        }
    }
}
