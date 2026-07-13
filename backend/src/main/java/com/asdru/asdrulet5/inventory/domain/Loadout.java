package com.asdru.asdrulet5.inventory.domain;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * A party member's equipped gear: at most one item per slot. Item IDs are
 * stored as opaque strings (resolved against ItemDefinitionRegistry by
 * callers) rather than embedding ItemDefinition directly, mirroring how
 * PartyMember stores a CharacterClass enum rather than a ClassDefinition.
 */
public record Loadout(String weaponItemId, String chestplateItemId, String trinketItemId) {

    public static Loadout empty() {
        return new Loadout(null, null, null);
    }

    public Loadout withItem(ItemSlot slot, String itemId) {
        return switch (slot) {
            case WEAPON -> new Loadout(itemId, chestplateItemId, trinketItemId);
            case CHESTPLATE -> new Loadout(weaponItemId, itemId, trinketItemId);
            case TRINKET -> new Loadout(weaponItemId, chestplateItemId, itemId);
        };
    }

    public String itemIdFor(ItemSlot slot) {
        return switch (slot) {
            case WEAPON -> weaponItemId;
            case CHESTPLATE -> chestplateItemId;
            case TRINKET -> trinketItemId;
        };
    }

    public List<String> equippedItemIds() {
        return Stream.of(weaponItemId, chestplateItemId, trinketItemId)
                .filter(Objects::nonNull)
                .toList();
    }
}
