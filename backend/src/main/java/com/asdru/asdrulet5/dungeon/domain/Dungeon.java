package com.asdru.asdrulet5.dungeon.domain;

import com.asdru.asdrulet5.dungeon.exception.InvalidNodeTransitionException;
import com.asdru.asdrulet5.party.exception.NotPartyLeaderException;
import lombok.Getter;
import lombok.Synchronized;
import lombok.experimental.Accessors;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Dungeon {

    @Getter
    @Accessors(fluent = true)
    private final String code;

    /**
     * Copied from Party.leaderId() at generation time so this aggregate can
     * enforce its own leader check without reaching back into Party, mirroring
     * how Party itself is self-contained.
     */
    @Getter
    @Accessors(fluent = true)
    private final String leaderId;

    private final Map<String, DungeonNode> nodesById = new LinkedHashMap<>();

    @Getter
    @Accessors(fluent = true)
    private String currentNodeId;

    private final Set<String> visitedNodeIds = new LinkedHashSet<>();

    public Dungeon(String code, String leaderId, List<DungeonNode> nodes, String startNodeId) {
        this.code = code;
        this.leaderId = leaderId;
        nodes.forEach(node -> nodesById.put(node.id(), node));
        this.currentNodeId = startNodeId;
        this.visitedNodeIds.add(startNodeId);
    }

    @Synchronized
    public List<DungeonNode> nodes() {
        return List.copyOf(nodesById.values());
    }

    @Synchronized
    public Set<String> visitedNodeIds() {
        return Set.copyOf(visitedNodeIds);
    }

    @Synchronized
    public List<String> availableNodeIds() {
        return nodesById.get(currentNodeId).nextNodeIds();
    }

    @Synchronized
    public void moveTo(String requesterId, String targetNodeId) {
        if (!leaderId.equals(requesterId)) {
            throw new NotPartyLeaderException(code, requesterId);
        }
        DungeonNode current = nodesById.get(currentNodeId);
        if (!current.nextNodeIds().contains(targetNodeId)) {
            throw new InvalidNodeTransitionException(code, currentNodeId, targetNodeId);
        }
        this.currentNodeId = targetNodeId;
        this.visitedNodeIds.add(targetNodeId);
    }
}
