package com.asdru.asdrulet5.dungeon.web;

import com.asdru.asdrulet5.dungeon.domain.Dungeon;
import com.asdru.asdrulet5.dungeon.domain.DungeonNode;
import com.asdru.asdrulet5.dungeon.web.dto.DungeonNodeDto;
import com.asdru.asdrulet5.dungeon.web.dto.DungeonStateDto;
import lombok.experimental.UtilityClass;

@UtilityClass
public class DungeonMapper {

    public DungeonStateDto toDto(Dungeon dungeon) {
        return new DungeonStateDto(
                dungeon.code(),
                dungeon.nodes().stream().map(DungeonMapper::toDto).toList(),
                dungeon.currentNodeId(),
                dungeon.availableNodeIds(),
                dungeon.visitedNodeIds()
        );
    }

    private DungeonNodeDto toDto(DungeonNode node) {
        return new DungeonNodeDto(node.id(), node.roomType(), node.layer(), node.indexInLayer(), node.nextNodeIds());
    }
}
