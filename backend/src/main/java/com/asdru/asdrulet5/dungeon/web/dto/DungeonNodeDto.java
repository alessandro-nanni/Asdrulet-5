package com.asdru.asdrulet5.dungeon.web.dto;

import com.asdru.asdrulet5.dungeon.domain.RoomType;

import java.util.List;

public record DungeonNodeDto(
        String id,
        RoomType roomType,
        int layer,
        int indexInLayer,
        List<String> nextNodeIds
) {
}
