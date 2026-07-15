package com.asdru.asdrulet5.enemydata;

import com.asdru.asdrulet5.enemydata.domain.EncounterSize;
import com.asdru.asdrulet5.enemydata.domain.EncounterTable;
import com.asdru.asdrulet5.enemydata.domain.EnemyPoolEntry;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Static catalog of which enemies spawn together in a regular (non-boss)
 * fight, and how many — one {@link EncounterTable} per dungeon floor, mixed
 * and matched from {@link EnemyDefinitionRegistry}'s catalog. Kept separate
 * from that registry for the same reason {@code LootTableRegistry} is kept
 * separate from {@code ItemDefinitionRegistry}: an enemy's stats/attack
 * don't change based on where it's encountered, but which enemies show up
 * together and how often is exactly that kind of context-dependent,
 * encounter-table concern.
 *
 * <p>Boss encounters don't go through this table at all — {@code
 * CombatService} spawns {@link EnemyDefinitionRegistry#DEFAULT_ENEMY_ID}
 * alone for those, unaffected by whatever's configured here.
 *
 * <p>The dungeon doesn't have distinct floors yet, so only {@link #FLOOR_1}
 * exists — a floor 2 later is just another entry in {@link #ENCOUNTER_TABLES}
 * with its own mix of enemies, weights, and encounter size.
 */
@Component
public class EnemyEncounterRegistry {

    private static final int FLOOR_1 = 1;

    private static final Map<Integer, EncounterTable> ENCOUNTER_TABLES = buildTables();
    private static final EncounterTable EMPTY_TABLE = new EncounterTable(List.of(), EncounterSize.fixed(1));

    private static Map<Integer, EncounterTable> buildTables() {
        List<EnemyPoolEntry> entries = List.of(
                new EnemyPoolEntry("cave-rat", 35),
                new EnemyPoolEntry("goblin-skirmisher", 25),
                new EnemyPoolEntry("bandit-thug", 20)
        );
        return Map.of(FLOOR_1, new EncounterTable(entries, new EncounterSize(2, 3)));
    }

    /**
     * This floor's encounter table, or an all-empty one if nothing's been assigned to it yet.
     */
    public EncounterTable forFloor(int floor) {
        return ENCOUNTER_TABLES.getOrDefault(floor, EMPTY_TABLE);
    }
}
