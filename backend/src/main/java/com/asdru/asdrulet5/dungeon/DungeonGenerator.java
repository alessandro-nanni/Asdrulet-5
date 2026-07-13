package com.asdru.asdrulet5.dungeon;

import com.asdru.asdrulet5.dungeon.domain.DungeonNode;
import com.asdru.asdrulet5.dungeon.domain.RoomType;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds a small procedurally-generated dungeon: a layered directed acyclic
 * graph with a single START node, a single BOSS node, and 3-4 branching
 * middle layers in between. Every node in a layer is guaranteed at least one
 * incoming edge from the previous layer, so the whole graph is reachable from
 * START.
 */
@Component
public class DungeonGenerator {

    private static final int MIN_MIDDLE_LAYERS = 3;
    private static final int MAX_MIDDLE_LAYERS = 4;
    private static final int MIN_NODES_PER_LAYER = 2;
    private static final int MAX_NODES_PER_LAYER = 3;
    private static final int MAX_FORWARD_EDGES = 2;

    private final SecureRandom random = new SecureRandom();

    public List<DungeonNode> generate() {
        int middleLayers = MIN_MIDDLE_LAYERS + random.nextInt(MAX_MIDDLE_LAYERS - MIN_MIDDLE_LAYERS + 1);
        int layerCount = middleLayers + 2;

        List<List<String>> idsByLayer = new ArrayList<>();
        int idSequence = 0;
        for (int layer = 0; layer < layerCount; layer++) {
            int nodesInLayer = isTerminalLayer(layer, layerCount)
                    ? 1
                    : MIN_NODES_PER_LAYER + random.nextInt(MAX_NODES_PER_LAYER - MIN_NODES_PER_LAYER + 1);
            List<String> ids = new ArrayList<>();
            for (int i = 0; i < nodesInLayer; i++) {
                ids.add("n" + idSequence++);
            }
            idsByLayer.add(ids);
        }

        Map<String, Set<String>> forwardEdges = new LinkedHashMap<>();
        for (int layer = 0; layer < layerCount - 1; layer++) {
            List<String> current = idsByLayer.get(layer);
            List<String> next = idsByLayer.get(layer + 1);

            for (String fromId : current) {
                int edgeCount = Math.min(next.size(), 1 + random.nextInt(MAX_FORWARD_EDGES));
                forwardEdges.put(fromId, pickRandomDistinct(next, edgeCount));
            }

            for (String targetId : next) {
                boolean hasIncoming = current.stream().anyMatch(fromId -> forwardEdges.get(fromId).contains(targetId));
                if (!hasIncoming) {
                    String fromId = current.get(random.nextInt(current.size()));
                    forwardEdges.get(fromId).add(targetId);
                }
            }
        }

        List<DungeonNode> nodes = new ArrayList<>();
        for (int layer = 0; layer < layerCount; layer++) {
            List<String> ids = idsByLayer.get(layer);
            for (int i = 0; i < ids.size(); i++) {
                String id = ids.get(i);
                RoomType roomType = roomTypeFor(layer, layerCount);
                List<String> nextNodeIds = List.copyOf(forwardEdges.getOrDefault(id, Set.of()));
                nodes.add(new DungeonNode(id, roomType, layer, i, nextNodeIds));
            }
        }
        return nodes;
    }

    private boolean isTerminalLayer(int layer, int layerCount) {
        return layer == 0 || layer == layerCount - 1;
    }

    private RoomType roomTypeFor(int layer, int layerCount) {
        if (layer == 0) {
            return RoomType.START;
        }
        if (layer == layerCount - 1) {
            return RoomType.BOSS;
        }
        int roll = random.nextInt(100);
        if (roll < 55) {
            return RoomType.FIGHT;
        }
        if (roll < 80) {
            return RoomType.LOOT;
        }
        return RoomType.MERCHANT;
    }

    private Set<String> pickRandomDistinct(List<String> candidates, int count) {
        List<String> shuffled = new ArrayList<>(candidates);
        Collections.shuffle(shuffled, random);
        return new LinkedHashSet<>(shuffled.subList(0, count));
    }
}
