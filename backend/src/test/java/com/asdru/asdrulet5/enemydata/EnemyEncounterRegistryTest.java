package com.asdru.asdrulet5.enemydata;

import com.asdru.asdrulet5.enemydata.domain.EncounterSize;
import com.asdru.asdrulet5.enemydata.domain.EncounterTable;
import com.asdru.asdrulet5.enemydata.domain.EnemyPoolEntry;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class EnemyEncounterRegistryTest {

    private final EnemyEncounterRegistry enemyEncounterRegistry = new EnemyEncounterRegistry();
    private final EnemyDefinitionRegistry enemyDefinitionRegistry = new EnemyDefinitionRegistry();

    @Test
    void floorOneTableDrawsBetweenTwoAndThreeEnemiesForASoloParty() {
        EncounterTable floor1 = enemyEncounterRegistry.forFloor(1);
        Random random = new Random(1);

        for (int i = 0; i < 50; i++) {
            assertThat(floor1.roll(random, EncounterSize.BASELINE_PARTY_SIZE).size()).isBetween(2, 3);
        }
    }

    @Test
    void floorOneTableDrawsMoreEnemiesForABiggerParty() {
        EncounterTable floor1 = enemyEncounterRegistry.forFloor(1);
        Random random = new Random(1);

        for (int i = 0; i < 50; i++) {
            assertThat(floor1.roll(random, 4).size()).isBetween(5, 6);
        }
    }

    @Test
    void everyFloorOneEntryReferencesARealEnemyDefinition() {
        for (EnemyPoolEntry entry : enemyEncounterRegistry.forFloor(1).entries()) {
            assertThat(enemyDefinitionRegistry.get(entry.enemyId())).isNotNull();
        }
    }

    @Test
    void floorOneTableDoesNotIncludeTheBossEnemy() {
        assertThat(enemyEncounterRegistry.forFloor(1).entries())
                .extracting(EnemyPoolEntry::enemyId)
                .doesNotContain(EnemyDefinitionRegistry.DEFAULT_ENEMY_ID);
    }

    @Test
    void tableIsEmptyForAFloorThatDoesntExistYet() {
        assertThat(enemyEncounterRegistry.forFloor(2).roll(new Random(1), EncounterSize.BASELINE_PARTY_SIZE)).isEmpty();
    }
}
