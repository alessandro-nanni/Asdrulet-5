package com.asdru.asdrulet5.dungeon;

import com.asdru.asdrulet5.dungeon.domain.DungeonNode;
import com.asdru.asdrulet5.dungeon.domain.RoomType;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.*;

/**
 * Builds a procedurally-generated dungeon: a layered directed acyclic graph
 * with a single START node, a single BOSS node, and several branching middle
 * layers in between. Every node in a layer is guaranteed at least one
 * incoming edge from the previous layer, so the whole graph is reachable from
 * START. Occasional "skip" edges jump two layers ahead to add shortcuts and
 * make the branching feel less uniform, but the graph always still funnels
 * into the single terminal BOSS node.
 */
@Component
public class DungeonGenerator {

    private static final int MIN_MIDDLE_LAYERS = 4;
    private static final int MAX_MIDDLE_LAYERS = 6;
    private static final int MIN_NODES_PER_LAYER = 2;
    private static final int MAX_NODES_PER_LAYER = 4;
    private static final int MAX_FORWARD_EDGES = 3;
    private static final double SKIP_EDGE_CHANCE = 0.25;

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

        // Extra shortcut edges two layers ahead: purely additive branching on
        // top of the guaranteed-reachable layer-by-layer skeleton above, so
        // they can never strand a node or break the single-BOSS invariant.
        for (int layer = 0; layer < layerCount - 2; layer++) {
            List<String> current = idsByLayer.get(layer);
            List<String> skipTarget = idsByLayer.get(layer + 2);
            for (String fromId : current) {
                if (random.nextDouble() < SKIP_EDGE_CHANCE) {
                    String targetId = skipTarget.get(random.nextInt(skipTarget.size()));
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
        // Weighted toward combat: fights should be the room you run into most.
        int roll = random.nextInt(100);
        if (roll < 55) {
            return RoomType.FIGHT;
        }
        if (roll < 73) {
            return RoomType.LOOT;
        }
        if (roll < 88) {
            return RoomType.MYSTERY;
        }
        return RoomType.MERCHANT;
    }

    private Set<String> pickRandomDistinct(List<String> candidates, int count) {
        List<String> shuffled = new ArrayList<>(candidates);
        Collections.shuffle(shuffled, random);
        return new LinkedHashSet<>(shuffled.subList(0, count));
    }
}
