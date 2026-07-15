package com.asdru.asdrulet5.inventory;

import com.asdru.asdrulet5.inventory.domain.ItemSlot;
import com.asdru.asdrulet5.inventory.exception.UnknownItemDefinitionException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ItemDefinitionRegistryTest {

    private final ItemDefinitionRegistry registry = new ItemDefinitionRegistry();

    @Test
    void everySlotHasAtLeastOneItem() {
        for (ItemSlot slot : ItemSlot.values()) {
            assertThat(registry.all()).anyMatch(item -> item.slot() == slot);
        }
    }

    @Test
    void getReturnsTheMatchingDefinition() {
        assertThat(registry.get("scythe").slot()).isEqualTo(ItemSlot.WEAPON);
    }

    @Test
    void unknownIdThrows() {
        assertThatThrownBy(() -> registry.get("nope"))
                .isInstanceOf(UnknownItemDefinitionException.class);
    }
}
