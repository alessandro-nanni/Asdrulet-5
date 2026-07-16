package com.asdru.asdrulet5.inventory;

import com.asdru.asdrulet5.inventory.domain.FloorLootTables;
import com.asdru.asdrulet5.inventory.domain.LootAmount;
import com.asdru.asdrulet5.inventory.domain.LootTable;
import com.asdru.asdrulet5.inventory.domain.LootTableEntry;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Static catalog of which items can drop, how many at a time, and where —
 * one {@link FloorLootTables} per dungeon floor, bundling that floor's own
 * chest/wheel/merchant tables so each floor can offer a completely
 * different mix of items (or weights, or draw amounts) in each of the
 * three. Kept separate from {@link ItemDefinitionRegistry}: an item's
 * mechanics don't change based on where it's obtained, but its drop odds,
 * draw count, and shop eligibility are exactly that kind of
 * context-dependent, drop-table concern — an item is "purchasable" simply
 * by having an entry in a floor's merchant table, nothing more.
 *
 * <p>The dungeon doesn't have distinct floors yet, so only {@link #FLOOR_1}
 * exists — adding a floor 2 later is just another entry in {@link #FLOORS}
 * with its own items, weights, and amounts.
 */
@Component
public class LootTableRegistry {

    private static final int FLOOR_1 = 1;

    private static final Map<Integer, FloorLootTables> FLOORS = buildFloors();
    private static final FloorLootTables EMPTY_FLOOR = new FloorLootTables(
            new LootTable(List.of(), LootAmount.fixed(1)),
            new LootTable(List.of(), LootAmount.fixed(1)),
            new LootTable(List.of(), LootAmount.fixed(1)));

    private static Map<Integer, FloorLootTables> buildFloors() {
        return Map.of(FLOOR_1, floor1());
    }

    private static FloorLootTables floor1() {
        // Every item currently sits in both the chest and wheel tables, at
        // the same weight — nothing stops a future floor's tables diverging.
        List<LootTableEntry> entries = List.of(
                new LootTableEntry("scythe", 15),
                new LootTableEntry("torch", 20),
                new LootTableEntry("lucky-charm", 8),
                new LootTableEntry("satellite-dish", 14),
                new LootTableEntry("twitching-talisman", 12),
                new LootTableEntry("leather-tunic", 20),
                new LootTableEntry("mantle-of-the-usurper", 10),
                new LootTableEntry("berserker-breastplate", 16)
        );
        return new FloorLootTables(
                new LootTable(entries, new LootAmount(1, 3)),
                new LootTable(entries, LootAmount.fixed(1)),
                new LootTable(List.of(new LootTableEntry("healing-potion", 1)), LootAmount.fixed(8)));
    }

    /**
     * This floor's chest/wheel/merchant tables, or all-empty tables if nothing's been assigned to it yet.
     */
    public FloorLootTables forFloor(int floor) {
        return FLOORS.getOrDefault(floor, EMPTY_FLOOR);
    }
}
