package com.asdru.asdrulet5.dungeon;

import com.asdru.asdrulet5.dungeon.domain.Dungeon;

import java.util.Optional;

public interface DungeonRepository {

    Dungeon save(Dungeon dungeon);

    Optional<Dungeon> findByCode(String code);
}
