package com.asdru.asdrulet5.dungeon;

import com.asdru.asdrulet5.dungeon.domain.Dungeon;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds all dungeon state purely in memory for the lifetime of the process,
 * keyed by party code — same pattern as InMemoryPartyRepository/InMemoryCombatRepository.
 */
@Repository
public class InMemoryDungeonRepository implements DungeonRepository {

    private final ConcurrentHashMap<String, Dungeon> dungeons = new ConcurrentHashMap<>();

    @Override
    public Dungeon save(Dungeon dungeon) {
        dungeons.put(dungeon.code(), dungeon);
        return dungeon;
    }

    @Override
    public Optional<Dungeon> findByCode(String code) {
        return Optional.ofNullable(dungeons.get(code));
    }
}
