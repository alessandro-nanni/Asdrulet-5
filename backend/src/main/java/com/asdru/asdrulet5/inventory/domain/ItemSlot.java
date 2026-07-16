package com.asdru.asdrulet5.inventory.domain;

public enum ItemSlot {
    WEAPON,
    CHESTPLATE,
    TRINKET,
    /**
     * Not equippable — sits only in shared storage until consumed (see
     * {@code Party.consumeItem}), never in a {@link Loadout}.
     */
    CONSUMABLE
}
