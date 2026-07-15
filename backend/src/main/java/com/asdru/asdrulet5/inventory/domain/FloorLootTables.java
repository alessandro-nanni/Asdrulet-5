package com.asdru.asdrulet5.inventory.domain;

/**
 * The three {@link LootTable}s that make up one dungeon floor's drop
 * config — one per place items are ever drawn from: a loot chest, the
 * mystery wheel, and the merchant. Grouped together (rather than three
 * parallel per-context maps) so {@code LootTableRegistry} has exactly one
 * entry per floor to look up, holding whatever mix of items/weights/amounts
 * that floor is meant to offer in each of the three.
 */
public record FloorLootTables(LootTable chestTable, LootTable wheelTable, LootTable merchantTable) {
}
