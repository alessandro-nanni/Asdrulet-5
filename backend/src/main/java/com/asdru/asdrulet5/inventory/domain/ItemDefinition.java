package com.asdru.asdrulet5.inventory.domain;

import java.util.Objects;

/**
 * What an item is and does — mechanics and flavor only. Where/how often it
 * can actually be obtained (shop-eligible, drop weight, which floor) is
 * deliberately kept out of here and owned instead by {@code LootPoolEntry}/
 * {@code LootPoolRegistry} — those are a drop-table concern (the same item
 * could reasonably have different odds in a loot chest vs. the wheel vs. the
 * shop later), not an intrinsic property of the item itself.
 */
public record ItemDefinition(
        String id,
        String displayName,
        ItemSlot slot,
        String description,
        ItemPassive passive,
        int price
) {
    public ItemDefinition {
        requireNonBlank(id, "id");
        requireNonBlank(displayName, "displayName");
        Objects.requireNonNull(slot, "slot");
        requireNonBlank(description, "description");
        Objects.requireNonNull(passive, "passive");
        if (price < 0) {
            throw new IllegalArgumentException("price must not be negative");
        }
    }

    private static void requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }
}
