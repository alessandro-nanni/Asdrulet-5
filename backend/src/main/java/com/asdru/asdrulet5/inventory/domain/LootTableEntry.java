package com.asdru.asdrulet5.inventory.domain;

/**
 * One item's membership in a {@link LootTable} — deliberately separate from
 * {@link ItemDefinition} itself, since how likely and where an item drops is
 * a drop-table concern, not a property of the item. Whether an item is
 * purchasable is no longer a flag here either: it's simply whether it has an
 * entry in a floor's merchant table (see {@code LootTableRegistry}) — the
 * same item can have a different weight (or not appear at all) across the
 * wheel/chest/merchant tables for the same floor.
 */
public record LootTableEntry(String itemId, int weight) {
    public LootTableEntry {
        if (itemId == null || itemId.isBlank()) {
            throw new IllegalArgumentException("itemId must not be blank");
        }
        if (weight <= 0) {
            throw new IllegalArgumentException("weight must be positive");
        }
    }
}
