package com.asdru.asdrulet5.dungeon.web.dto;

import java.util.List;
import java.util.Set;

public record DungeonStateDto(
        String code,
        List<DungeonNodeDto> nodes,
        String currentNodeId,
        List<String> availableNodeIds,
        Set<String> visitedNodeIds
) {
}
