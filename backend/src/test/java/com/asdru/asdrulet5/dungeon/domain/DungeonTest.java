package com.asdru.asdrulet5.dungeon.domain;

import com.asdru.asdrulet5.dungeon.exception.InvalidNodeTransitionException;
import com.asdru.asdrulet5.party.exception.NotPartyLeaderException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DungeonTest {

    @Test
    void constructorStartsAtGivenNodeAndMarksItVisited() {
        Dungeon dungeon = new Dungeon("ABC123", "leader-1", threeNodeChain(), "n0");

        assertThat(dungeon.currentNodeId()).isEqualTo("n0");
        assertThat(dungeon.visitedNodeIds()).containsExactly("n0");
        assertThat(dungeon.currentRoomType()).isEqualTo(RoomType.START);
        assertThat(dungeon.availableNodeIds()).containsExactly("n1");
    }

    @Test
    void moveToByNonLeaderThrows() {
        Dungeon dungeon = new Dungeon("ABC123", "leader-1", threeNodeChain(), "n0");

        assertThatThrownBy(() -> dungeon.moveTo("intruder", "n1"))
                .isInstanceOf(NotPartyLeaderException.class);
        assertThat(dungeon.currentNodeId()).isEqualTo("n0");
    }

    @Test
    void moveToNonAdjacentNodeThrows() {
        Dungeon dungeon = new Dungeon("ABC123", "leader-1", threeNodeChain(), "n0");

        assertThatThrownBy(() -> dungeon.moveTo("leader-1", "n2"))
                .isInstanceOf(InvalidNodeTransitionException.class);
    }

    @Test
    void moveToAdjacentNodeUpdatesCurrentAndVisited() {
        Dungeon dungeon = new Dungeon("ABC123", "leader-1", threeNodeChain(), "n0");

        dungeon.moveTo("leader-1", "n1");

        assertThat(dungeon.currentNodeId()).isEqualTo("n1");
        assertThat(dungeon.currentRoomType()).isEqualTo(RoomType.FIGHT);
        assertThat(dungeon.visitedNodeIds()).containsExactlyInAnyOrder("n0", "n1");
        assertThat(dungeon.availableNodeIds()).containsExactly("n2");
    }

    @Test
    void moveToAlreadyVisitedNodeThrowsEvenIfListedAsNext() {
        // A back-edge (n1 -> n0) that would never occur in a real generated
        // graph, constructed here specifically to prove the visited-node
        // guard is independent of the graph's own shape.
        List<DungeonNode> nodes = List.of(
                new DungeonNode("n0", RoomType.START, 0, 0, List.of("n1")),
                new DungeonNode("n1", RoomType.FIGHT, 1, 0, List.of("n0", "n2")),
                new DungeonNode("n2", RoomType.BOSS, 2, 0, List.of())
        );
        Dungeon dungeon = new Dungeon("ABC123", "leader-1", nodes, "n0");
        dungeon.moveTo("leader-1", "n1");

        assertThatThrownBy(() -> dungeon.moveTo("leader-1", "n0"))
                .isInstanceOf(InvalidNodeTransitionException.class);
        assertThat(dungeon.currentNodeId()).isEqualTo("n1");
    }

    private List<DungeonNode> threeNodeChain() {
        return List.of(
                new DungeonNode("n0", RoomType.START, 0, 0, List.of("n1")),
                new DungeonNode("n1", RoomType.FIGHT, 1, 0, List.of("n2")),
                new DungeonNode("n2", RoomType.BOSS, 2, 0, List.of())
        );
    }
}
