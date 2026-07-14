package com.asdru.asdrulet5.dungeon.domain;

import com.asdru.asdrulet5.dungeon.exception.InvalidNodeTransitionException;
import com.asdru.asdrulet5.dungeon.exception.NoRoomSelectedException;
import com.asdru.asdrulet5.dungeon.exception.RoomAlreadyEnteredException;
import com.asdru.asdrulet5.party.exception.NotPartyLeaderException;
import lombok.Getter;
import lombok.Synchronized;
import lombok.experimental.Accessors;

import java.util.Collections;
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

    /**
     * The last node the party actually cleared — the anchor that browsing
     * always resets back to, and the base whose {@code nextNodeIds} define
     * what's selectable next.
     */
    @Getter
    @Accessors(fluent = true)
    private String homeNodeId;

    /**
     * Where the party is currently looking/standing: either {@link #homeNodeId}
     * itself or one of its next-room options. Freely reassignable via
     * {@link #select} as long as nothing is entered yet — this is what lets
     * the party bounce between the home room and any sibling option before
     * committing to one.
     */
    @Getter
    @Accessors(fluent = true)
    private String currentNodeId;

    /**
     * Null until {@link #enter} is called, at which point it's set to
     * (a copy of) {@link #currentNodeId} and browsing locks until
     * {@link #clearEnteredNode} resolves it — that's what enforces "the next
     * rooms can't be entered until the current one is cleared".
     */
    @Getter
    @Accessors(fluent = true)
    private String enteredNodeId;

    private final Set<String> clearedNodeIds = new LinkedHashSet<>();

    public Dungeon(String code, String leaderId, List<DungeonNode> nodes, String startNodeId) {
        this.code = code;
        this.leaderId = leaderId;
        nodes.forEach(node -> nodesById.put(node.id(), node));
        this.homeNodeId = startNodeId;
        this.currentNodeId = startNodeId;
        this.clearedNodeIds.add(startNodeId);
    }

    @Synchronized
    public List<DungeonNode> nodes() {
        return List.copyOf(nodesById.values());
    }

    @Synchronized
    public Set<String> clearedNodeIds() {
        // Set.copyOf does NOT preserve insertion order even when copying a
        // LinkedHashSet — its iteration order is explicitly unspecified —
        // and the frontend relies on this collection's order to know which
        // edges were actually taken (consecutive pairs), so an
        // order-preserving wrapper is required here, not just "any Set".
        return Collections.unmodifiableSet(new LinkedHashSet<>(clearedNodeIds));
    }

    @Synchronized
    public List<String> availableNodeIds() {
        return nodesById.get(homeNodeId).nextNodeIds();
    }

    @Synchronized
    public RoomType currentRoomType() {
        return nodesById.get(currentNodeId).roomType();
    }

    /**
     * Moves the browse position to {@code targetNodeId} — either back to
     * {@link #homeNodeId} or to any of its next-room options — without
     * committing to anything. Blocked once a room has been entered (browsing
     * only makes sense before that commitment).
     */
    @Synchronized
    public void select(String requesterId, String targetNodeId) {
        if (!leaderId.equals(requesterId)) {
            throw new NotPartyLeaderException(code, requesterId);
        }
        if (enteredNodeId != null) {
            throw new RoomAlreadyEnteredException(code);
        }
        boolean isHome = targetNodeId.equals(homeNodeId);
        // The graph's edges are already forward-only (a cleared node is never
        // relisted as a next-room option), but this check makes that
        // guarantee explicit and independent of the graph's own shape — a
        // previously-cleared node is never a valid browse target, full stop,
        // except homeNodeId itself which is always fair game to browse back
        // to.
        if (!isHome && (clearedNodeIds.contains(targetNodeId) || !availableNodeIds().contains(targetNodeId))) {
            throw new InvalidNodeTransitionException(code, homeNodeId, targetNodeId);
        }
        this.currentNodeId = targetNodeId;
    }

    /**
     * Commits to whichever next-room option is currently selected, locking
     * browsing until it's cleared.
     */
    @Synchronized
    public void enter(String requesterId) {
        if (!leaderId.equals(requesterId)) {
            throw new NotPartyLeaderException(code, requesterId);
        }
        if (enteredNodeId != null) {
            throw new RoomAlreadyEnteredException(code);
        }
        if (currentNodeId.equals(homeNodeId)) {
            throw new NoRoomSelectedException(code);
        }
        this.enteredNodeId = currentNodeId;
    }

    /**
     * Resolves whatever's currently entered — the entered node becomes the
     * new home, browsing unlocks and resets there, and the cycle can repeat
     * at the new home's next-room options.
     */
    @Synchronized
    public void clearEnteredNode() {
        if (enteredNodeId == null) {
            throw new IllegalStateException("No room is currently entered in party " + code);
        }
        this.homeNodeId = enteredNodeId;
        this.currentNodeId = enteredNodeId;
        this.clearedNodeIds.add(enteredNodeId);
        this.enteredNodeId = null;
    }
}
