package com.asdru.asdrulet5.dungeon.web.dto;

import java.util.List;
import java.util.Set;

public record DungeonStateDto(
        String code,
        List<DungeonNodeDto> nodes,
        String homeNodeId,
        String currentNodeId,
        String enteredNodeId,
        List<String> availableNodeIds,
        Set<String> clearedNodeIds
) {
}
