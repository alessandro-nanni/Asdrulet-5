package com.asdru.asdrulet5.party.domain;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RoomTurnProgressTest {

    @Test
    void freshProgressHasNobodyActedOrAcknowledged() {
        RoomTurnProgress<String> progress = new RoomTurnProgress<>();

        assertThat(progress.hasActed("p1")).isFalse();
        assertThat(progress.allMembersHaveActed(Set.of("p1"))).isFalse();
        assertThat(progress.allMembersHaveAcknowledged(Set.of("p1"))).isFalse();
        assertThat(progress.results()).isEmpty();
    }

    @Test
    void recordResultMarksActedAndStoresTheResult() {
        RoomTurnProgress<String> progress = new RoomTurnProgress<>();

        progress.recordResult("p1", "treasure");

        assertThat(progress.hasActed("p1")).isTrue();
        assertThat(progress.results()).containsEntry("p1", "treasure");
    }

    @Test
    void allMembersHaveActedOnlyOnceEveryGivenIdHasActed() {
        RoomTurnProgress<String> progress = new RoomTurnProgress<>();
        progress.recordResult("p1", "a");

        assertThat(progress.allMembersHaveActed(Set.of("p1", "p2"))).isFalse();

        progress.recordResult("p2", "b");

        assertThat(progress.allMembersHaveActed(Set.of("p1", "p2"))).isTrue();
    }

    @Test
    void isMembersTurnFollowsTurnOrderSequentially() {
        RoomTurnProgress<String> progress = new RoomTurnProgress<>();
        List<String> turnOrder = List.of("p2", "p1");

        assertThat(progress.isMembersTurn("p2", turnOrder)).isTrue();
        assertThat(progress.isMembersTurn("p1", turnOrder)).isFalse();

        progress.recordResult("p2", "a");

        assertThat(progress.isMembersTurn("p1", turnOrder)).isTrue();
        assertThat(progress.isMembersTurn("p2", turnOrder)).isFalse();
    }

    @Test
    void isMembersTurnIsFalseForEveryoneOnceAllHaveActed() {
        RoomTurnProgress<String> progress = new RoomTurnProgress<>();
        List<String> turnOrder = List.of("p1");
        progress.recordResult("p1", "a");

        assertThat(progress.isMembersTurn("p1", turnOrder)).isFalse();
    }

    @Test
    void acknowledgementTrackingIsIndependentFromActing() {
        RoomTurnProgress<String> progress = new RoomTurnProgress<>();
        progress.recordResult("p1", "a");

        progress.recordAcknowledgement("p1");
        assertThat(progress.allMembersHaveAcknowledged(Set.of("p1", "p2"))).isFalse();

        progress.recordAcknowledgement("p2");
        assertThat(progress.allMembersHaveAcknowledged(Set.of("p1", "p2"))).isTrue();
    }

    @Test
    void resetClearsActedResultsAndAcknowledgements() {
        RoomTurnProgress<String> progress = new RoomTurnProgress<>();
        progress.recordResult("p1", "a");
        progress.recordAcknowledgement("p1");

        progress.reset();

        assertThat(progress.hasActed("p1")).isFalse();
        assertThat(progress.results()).isEmpty();
        assertThat(progress.allMembersHaveAcknowledged(Set.of("p1"))).isFalse();
    }
}
