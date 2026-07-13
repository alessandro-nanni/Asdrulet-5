package com.asdru.asdrulet5.inventory.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LoadoutTest {

    @Test
    void emptyHasNoEquippedItems() {
        assertThat(Loadout.empty().equippedItemIds()).isEmpty();
    }

    @Test
    void withItemFillsOnlyTheTargetSlot() {
        Loadout loadout = Loadout.empty().withItem(ItemSlot.WEAPON, "rusted-sword");

        assertThat(loadout.itemIdFor(ItemSlot.WEAPON)).isEqualTo("rusted-sword");
        assertThat(loadout.itemIdFor(ItemSlot.CHESTPLATE)).isNull();
        assertThat(loadout.itemIdFor(ItemSlot.TRINKET)).isNull();
        assertThat(loadout.equippedItemIds()).containsExactly("rusted-sword");
    }

    @Test
    void withItemReplacesWhateverWasInThatSlot() {
        Loadout loadout = Loadout.empty()
                .withItem(ItemSlot.WEAPON, "rusted-sword")
                .withItem(ItemSlot.WEAPON, "flame-edge");

        assertThat(loadout.itemIdFor(ItemSlot.WEAPON)).isEqualTo("flame-edge");
        assertThat(loadout.equippedItemIds()).containsExactly("flame-edge");
    }

    @Test
    void allThreeSlotsCanBeFilledIndependently() {
        Loadout loadout = Loadout.empty()
                .withItem(ItemSlot.WEAPON, "rusted-sword")
                .withItem(ItemSlot.CHESTPLATE, "leather-vest")
                .withItem(ItemSlot.TRINKET, "lucky-charm");

        assertThat(loadout.equippedItemIds()).containsExactlyInAnyOrder("rusted-sword", "leather-vest", "lucky-charm");
    }
}
