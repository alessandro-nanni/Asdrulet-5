package com.asdru.asdrulet5.party.domain;

import com.asdru.asdrulet5.party.exception.ClassAlreadyTakenException;
import com.asdru.asdrulet5.party.exception.InvalidTurnOrderException;
import com.asdru.asdrulet5.party.exception.NotPartyLeaderException;
import com.asdru.asdrulet5.party.exception.NotPartyMemberException;
import com.asdru.asdrulet5.party.exception.PartyFullException;
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
    void startByNonLeaderThrows() {
        Party party = new Party("ABC123", "leader-1", "Leader", "avatar.png");
        party.addMember("player-2", "Player Two", "avatar2.png");

        assertThatThrownBy(() -> party.start("player-2", List.of("leader-1", "player-2")))
                .isInstanceOf(NotPartyLeaderException.class);
        assertThat(party.status()).isEqualTo(PartyStatus.LOBBY);
    }

    @Test
    void startWithMissingMemberThrows() {
        Party party = new Party("ABC123", "leader-1", "Leader", "avatar.png");
        party.addMember("player-2", "Player Two", "avatar2.png");

        assertThatThrownBy(() -> party.start("leader-1", List.of("leader-1")))
                .isInstanceOf(InvalidTurnOrderException.class);
    }

    @Test
    void startWithDuplicateThrows() {
        Party party = new Party("ABC123", "leader-1", "Leader", "avatar.png");
        party.addMember("player-2", "Player Two", "avatar2.png");

        assertThatThrownBy(() -> party.start("leader-1", List.of("leader-1", "leader-1")))
                .isInstanceOf(InvalidTurnOrderException.class);
    }

    @Test
    void startByLeaderWithValidPermutationSetsOrderAndStatus() {
        Party party = new Party("ABC123", "leader-1", "Leader", "avatar.png");
        party.addMember("player-2", "Player Two", "avatar2.png");
        assertThat(party.status()).isEqualTo(PartyStatus.LOBBY);

        party.start("leader-1", List.of("player-2", "leader-1"));

        assertThat(party.turnOrder()).containsExactly("player-2", "leader-1");
        assertThat(party.status()).isEqualTo(PartyStatus.IN_PROGRESS);
    }

    @Test
    void addFakeMemberCreatesNonLeaderBotWithUniqueId() {
        Party party = new Party("ABC123", "leader-1", "Leader", "avatar.png");

        PartyMember bot1 = party.addFakeMember("Grog");
        PartyMember bot2 = party.addFakeMember("Brynn");

        assertThat(bot1.bot()).isTrue();
        assertThat(bot1.leader()).isFalse();
        assertThat(bot1.userId()).isNotEqualTo(bot2.userId());
        assertThat(party.members()).hasSize(3);
    }

    @Test
    void selectClassWorksForFakeMembersLikeAnyOtherMember() {
        Party party = new Party("ABC123", "leader-1", "Leader", "avatar.png");
        PartyMember bot = party.addFakeMember("Grog");

        party.selectClass(bot.userId(), CharacterClass.TANK);

        PartyMember updated = party.members().stream()
                .filter(member -> member.userId().equals(bot.userId()))
                .findFirst()
                .orElseThrow();
        assertThat(updated.characterClass()).isEqualTo(CharacterClass.TANK);
    }

    @Test
    void addMemberRejectsJoiningBeyondMaxMembers() {
        Party party = new Party("ABC123", "leader-1", "Leader", "avatar.png");
        party.addMember("player-2", "Player Two", "avatar2.png");
        party.addMember("player-3", "Player Three", "avatar3.png");
        party.addMember("player-4", "Player Four", "avatar4.png");

        assertThatThrownBy(() -> party.addMember("player-5", "Player Five", "avatar5.png"))
                .isInstanceOf(PartyFullException.class);
        assertThat(party.members()).hasSize(Party.MAX_MEMBERS);
    }

    @Test
    void addMemberStaysIdempotentEvenWhenPartyIsFull() {
        Party party = new Party("ABC123", "leader-1", "Leader", "avatar.png");
        party.addMember("player-2", "Player Two", "avatar2.png");
        party.addMember("player-3", "Player Three", "avatar3.png");
        party.addMember("player-4", "Player Four", "avatar4.png");

        PartyMember rejoined = party.addMember("player-2", "Player Two", "avatar2.png");

        assertThat(rejoined.userId()).isEqualTo("player-2");
        assertThat(party.members()).hasSize(Party.MAX_MEMBERS);
    }

    @Test
    void addFakeMemberRejectsBeyondMaxMembers() {
        Party party = new Party("ABC123", "leader-1", "Leader", "avatar.png");
        party.addFakeMember("Grog");
        party.addFakeMember("Brynn");
        party.addFakeMember("Thistle");

        assertThatThrownBy(() -> party.addFakeMember("Kael"))
                .isInstanceOf(PartyFullException.class);
        assertThat(party.members()).hasSize(Party.MAX_MEMBERS);
    }
}
