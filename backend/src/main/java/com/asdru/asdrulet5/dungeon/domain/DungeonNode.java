package com.asdru.asdrulet5.dungeon.domain;

import java.util.List;

public record DungeonNode(
        String id,
        RoomType roomType,
        int layer,
        int indexInLayer,
        List<String> nextNodeIds
) {
}
