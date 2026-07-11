package com.asdru.asdrulet5.party.domain;

import com.asdru.asdrulet5.party.exception.ClassAlreadyTakenException;
import com.asdru.asdrulet5.party.exception.InvalidTurnOrderException;
import com.asdru.asdrulet5.party.exception.NotPartyLeaderException;
import com.asdru.asdrulet5.party.exception.NotPartyMemberException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PartyTest {

    @Test
    void creatingPartyRegistersLeaderAsFirstMember() {
        Party party = new Party("ABC123", "leader-1", "Leader", "avatar.png");

        assertThat(party.members()).hasSize(1);
        PartyMember leader = party.members().get(0);
        assertThat(leader.userId()).isEqualTo("leader-1");
        assertThat(leader.leader()).isTrue();
        assertThat(leader.characterClass()).isNull();
    }

    @Test
    void addMemberJoinsAsNonLeader() {
        Party party = new Party("ABC123", "leader-1", "Leader", "avatar.png");

        party.addMember("player-2", "Player Two", "avatar2.png");

        assertThat(party.members()).hasSize(2);
        PartyMember joined = party.members().get(1);
        assertThat(joined.leader()).isFalse();
    }

    @Test
    void addMemberIsIdempotentForSameUser() {
        Party party = new Party("ABC123", "leader-1", "Leader", "avatar.png");

        party.addMember("player-2", "Player Two", "avatar2.png");
        party.addMember("player-2", "Player Two", "avatar2.png");

        assertThat(party.members()).hasSize(2);
    }

    @Test
    void selectClassUpdatesMember() {
        Party party = new Party("ABC123", "leader-1", "Leader", "avatar.png");

        party.selectClass("leader-1", CharacterClass.MAGE);

        assertThat(party.members().get(0).characterClass()).isEqualTo(CharacterClass.MAGE);
    }

    @Test
    void selectClassForUnknownUserThrows() {
        Party party = new Party("ABC123", "leader-1", "Leader", "avatar.png");

        assertThatThrownBy(() -> party.selectClass("nobody", CharacterClass.TANK))
                .isInstanceOf(NotPartyMemberException.class);
    }

    @Test
    void selectClassAlreadyTakenByAnotherMemberThrows() {
        Party party = new Party("ABC123", "leader-1", "Leader", "avatar.png");
        party.addMember("player-2", "Player Two", "avatar2.png");
        party.selectClass("leader-1", CharacterClass.WARRIOR);

        assertThatThrownBy(() -> party.selectClass("player-2", CharacterClass.WARRIOR))
                .isInstanceOf(ClassAlreadyTakenException.class);
    }

    @Test
    void reselectingOwnCurrentClassIsAllowed() {
        Party party = new Party("ABC123", "leader-1", "Leader", "avatar.png");
        party.selectClass("leader-1", CharacterClass.WARRIOR);

        party.selectClass("leader-1", CharacterClass.WARRIOR);

        assertThat(party.members().get(0).characterClass()).isEqualTo(CharacterClass.WARRIOR);
    }

    @Test
    void setTurnOrderByNonLeaderThrows() {
        Party party = new Party("ABC123", "leader-1", "Leader", "avatar.png");
        party.addMember("player-2", "Player Two", "avatar2.png");

        assertThatThrownBy(() -> party.setTurnOrder("player-2", List.of("leader-1", "player-2")))
                .isInstanceOf(NotPartyLeaderException.class);
    }

    @Test
    void setTurnOrderWithMissingMemberThrows() {
        Party party = new Party("ABC123", "leader-1", "Leader", "avatar.png");
        party.addMember("player-2", "Player Two", "avatar2.png");

        assertThatThrownBy(() -> party.setTurnOrder("leader-1", List.of("leader-1")))
                .isInstanceOf(InvalidTurnOrderException.class);
    }

    @Test
    void setTurnOrderWithDuplicateThrows() {
        Party party = new Party("ABC123", "leader-1", "Leader", "avatar.png");
        party.addMember("player-2", "Player Two", "avatar2.png");

        assertThatThrownBy(() -> party.setTurnOrder("leader-1", List.of("leader-1", "leader-1")))
                .isInstanceOf(InvalidTurnOrderException.class);
    }

    @Test
    void setTurnOrderByLeaderWithValidPermutationSucceeds() {
        Party party = new Party("ABC123", "leader-1", "Leader", "avatar.png");
        party.addMember("player-2", "Player Two", "avatar2.png");

        party.setTurnOrder("leader-1", List.of("player-2", "leader-1"));

        assertThat(party.turnOrder()).containsExactly("player-2", "leader-1");
    }
}
