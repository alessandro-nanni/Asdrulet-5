package com.asdru.asdrulet5.dungeon.domain;

import com.asdru.asdrulet5.dungeon.exception.InvalidNodeTransitionException;
import com.asdru.asdrulet5.dungeon.exception.NoRoomSelectedException;
import com.asdru.asdrulet5.dungeon.exception.RoomAlreadyEnteredException;
import com.asdru.asdrulet5.party.exception.NotPartyLeaderException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DungeonTest {

    @Test
    void constructorStartsAtGivenNodeAndMarksItCleared() {
        Dungeon dungeon = new Dungeon("ABC123", "leader-1", threeNodeChain(), "n0");

        assertThat(dungeon.homeNodeId()).isEqualTo("n0");
        assertThat(dungeon.currentNodeId()).isEqualTo("n0");
        assertThat(dungeon.enteredNodeId()).isNull();
        assertThat(dungeon.clearedNodeIds()).containsExactly("n0");
        assertThat(dungeon.currentRoomType()).isEqualTo(RoomType.START);
        assertThat(dungeon.availableNodeIds()).containsExactly("n1");
    }

    @Test
    void selectByNonLeaderThrows() {
        Dungeon dungeon = new Dungeon("ABC123", "leader-1", threeNodeChain(), "n0");

        assertThatThrownBy(() -> dungeon.select("intruder", "n1"))
                .isInstanceOf(NotPartyLeaderException.class);
        assertThat(dungeon.currentNodeId()).isEqualTo("n0");
    }

    @Test
    void selectNonAdjacentNodeThrows() {
        Dungeon dungeon = new Dungeon("ABC123", "leader-1", threeNodeChain(), "n0");

        assertThatThrownBy(() -> dungeon.select("leader-1", "n2"))
                .isInstanceOf(InvalidNodeTransitionException.class);
    }

    @Test
    void selectMovesBrowsePositionWithoutTouchingHome() {
        Dungeon dungeon = new Dungeon("ABC123", "leader-1", threeNodeChain(), "n0");

        dungeon.select("leader-1", "n1");

        assertThat(dungeon.currentNodeId()).isEqualTo("n1");
        assertThat(dungeon.homeNodeId()).isEqualTo("n0");
        assertThat(dungeon.clearedNodeIds()).containsExactly("n0");
    }

    @Test
    void selectCanReturnToHomeAfterBrowsingAnOption() {
        Dungeon dungeon = new Dungeon("ABC123", "leader-1", threeNodeChain(), "n0");
        dungeon.select("leader-1", "n1");

        dungeon.select("leader-1", "n0");

        assertThat(dungeon.currentNodeId()).isEqualTo("n0");
    }

    @Test
    void enterByNonLeaderThrows() {
        Dungeon dungeon = new Dungeon("ABC123", "leader-1", threeNodeChain(), "n0");
        dungeon.select("leader-1", "n1");

        assertThatThrownBy(() -> dungeon.enter("intruder"))
                .isInstanceOf(NotPartyLeaderException.class);
    }

    @Test
    void enterWithoutSelectingNextRoomThrows() {
        Dungeon dungeon = new Dungeon("ABC123", "leader-1", threeNodeChain(), "n0");

        assertThatThrownBy(() -> dungeon.enter("leader-1"))
                .isInstanceOf(NoRoomSelectedException.class);
    }

    @Test
    void enterLocksBrowsingUntilCleared() {
        Dungeon dungeon = new Dungeon("ABC123", "leader-1", threeNodeChain(), "n0");
        dungeon.select("leader-1", "n1");

        dungeon.enter("leader-1");

        assertThat(dungeon.enteredNodeId()).isEqualTo("n1");
        assertThatThrownBy(() -> dungeon.select("leader-1", "n0"))
                .isInstanceOf(RoomAlreadyEnteredException.class);
        assertThatThrownBy(() -> dungeon.enter("leader-1"))
                .isInstanceOf(RoomAlreadyEnteredException.class);
    }

    @Test
    void clearEnteredNodeAdvancesHomeAndUnlocksBrowsing() {
        Dungeon dungeon = new Dungeon("ABC123", "leader-1", threeNodeChain(), "n0");
        dungeon.select("leader-1", "n1");
        dungeon.enter("leader-1");

        dungeon.clearEnteredNode();

        assertThat(dungeon.homeNodeId()).isEqualTo("n1");
        assertThat(dungeon.currentNodeId()).isEqualTo("n1");
        assertThat(dungeon.enteredNodeId()).isNull();
        assertThat(dungeon.clearedNodeIds()).containsExactlyInAnyOrder("n0", "n1");
        assertThat(dungeon.availableNodeIds()).containsExactly("n2");
    }

    @Test
    void clearEnteredNodeWithoutAnythingEnteredThrows() {
        Dungeon dungeon = new Dungeon("ABC123", "leader-1", threeNodeChain(), "n0");

        assertThatThrownBy(dungeon::clearEnteredNode).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void previousHomeIsNoLongerSelectableOnceANewHomeIsCleared() {
        // A back-edge (n1 -> n0) that would never occur in a real generated
        // graph, constructed here specifically to prove old rooms stay
        // unreachable once the party has moved past them, independent of the
        // graph's own shape.
        List<DungeonNode> nodes = List.of(
                new DungeonNode("n0", RoomType.START, 0, 0, List.of("n1")),
                new DungeonNode("n1", RoomType.FIGHT, 1, 0, List.of("n0", "n2")),
                new DungeonNode("n2", RoomType.BOSS, 2, 0, List.of())
        );
        Dungeon dungeon = new Dungeon("ABC123", "leader-1", nodes, "n0");
        dungeon.select("leader-1", "n1");
        dungeon.enter("leader-1");
        dungeon.clearEnteredNode();

        assertThatThrownBy(() -> dungeon.select("leader-1", "n0"))
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
