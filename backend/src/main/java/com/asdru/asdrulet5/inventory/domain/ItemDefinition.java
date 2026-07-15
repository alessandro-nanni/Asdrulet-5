package com.asdru.asdrulet5.inventory.domain;

import java.util.Objects;

public record ItemDefinition(
        String id,
        String displayName,
        ItemSlot slot,
        String description,
        ItemPassive passive,
        int price,
        /** Whether the MERCHANT shop can ever offer this item — false for items meant to only turn up as loot (e.g. a mystery wheel GIVE_ITEM). */
        boolean purchasable
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
