package com.asdru.asdrulet5.inventory;

import com.asdru.asdrulet5.inventory.domain.FloorLootTables;
import com.asdru.asdrulet5.inventory.domain.LootTableEntry;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class LootTableRegistryTest {

    private final LootTableRegistry lootTableRegistry = new LootTableRegistry();
    private final ItemDefinitionRegistry itemDefinitionRegistry = new ItemDefinitionRegistry();

    @Test
    void wheelTableAlwaysDrawsExactlyOne() {
        FloorLootTables floor1 = lootTableRegistry.forFloor(1);
        Random random = new Random(1);

        for (int i = 0; i < 20; i++) {
            assertThat(floor1.wheelTable().roll(random)).hasSize(1);
        }
    }

    @Test
    void chestTableDrawsBetweenOneAndThree() {
        FloorLootTables floor1 = lootTableRegistry.forFloor(1);
        Random random = new Random(1);

        for (int i = 0; i < 50; i++) {
            assertThat(floor1.chestTable().roll(random).size()).isBetween(1, 3);
        }
    }

    @Test
    void merchantTableIsEmptyUntilAnItemIsMadePurchasable() {
        // No item is purchasable yet — see LootTableRegistry's own doc. An
        // empty table always rolls nothing, never throws.
        assertThat(lootTableRegistry.forFloor(1).merchantTable().roll(new Random(1))).isEmpty();
    }

    @Test
    void everyWheelAndChestEntryReferencesARealItemDefinition() {
        FloorLootTables floor1 = lootTableRegistry.forFloor(1);
        List<LootTableEntry> all = new ArrayList<>();
        all.addAll(floor1.wheelTable().entries());
        all.addAll(floor1.chestTable().entries());

        for (LootTableEntry entry : all) {
            assertThat(itemDefinitionRegistry.get(entry.itemId())).isNotNull();
        }
    }

    @Test
    void everyTableIsEmptyForAFloorThatDoesntExistYet() {
        FloorLootTables floor2 = lootTableRegistry.forFloor(2);

        assertThat(floor2.wheelTable().roll(new Random(1))).isEmpty();
        assertThat(floor2.chestTable().roll(new Random(1))).isEmpty();
        assertThat(floor2.merchantTable().roll(new Random(1))).isEmpty();
    }
}
