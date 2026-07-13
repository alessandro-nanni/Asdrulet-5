package com.asdru.asdrulet5.inventory.domain;

import java.util.Objects;

public record ItemDefinition(
        String id,
        String displayName,
        ItemSlot slot,
        String description,
        ItemPassive passive
) {
    public ItemDefinition {
        requireNonBlank(id, "id");
        requireNonBlank(displayName, "displayName");
        Objects.requireNonNull(slot, "slot");
        requireNonBlank(description, "description");
        Objects.requireNonNull(passive, "passive");
    }

    private static void requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }
}
