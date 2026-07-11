package com.asdru.asdrulet5.party;

import com.asdru.asdrulet5.auth.AuthenticatedUser;
import com.asdru.asdrulet5.combat.CombatService;
import com.asdru.asdrulet5.party.domain.CharacterClass;
import com.asdru.asdrulet5.party.domain.PartyStatus;
import com.asdru.asdrulet5.party.exception.ClassAlreadyTakenException;
import com.asdru.asdrulet5.party.exception.NotAFakeMemberException;
import com.asdru.asdrulet5.party.exception.NotPartyLeaderException;
import com.asdru.asdrulet5.party.exception.PartyNotFoundException;
import com.asdru.asdrulet5.party.web.dto.PartyMemberDto;
import com.asdru.asdrulet5.party.web.dto.PartyStateDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class PartyServiceTest {

    private final AuthenticatedUser leader = new AuthenticatedUser("leader-1", "Google Name", "leader.png");
    private final AuthenticatedUser member = new AuthenticatedUser("player-2", "Google Name Two", "player2.png");

    private PartyService partyService;
    private SimpMessagingTemplate messagingTemplate;

    @BeforeEach
    void setUp() {
        messagingTemplate = mock(SimpMessagingTemplate.class);
        partyService = new PartyService(new InMemoryPartyRepository(), messagingTemplate, mock(CombatService.class));
    }

    @Test
    void createPartyReturnsCodeWithLeaderAsOnlyMember() {
        PartyStateDto dto = partyService.createParty(leader, "Leader");

        assertThat(dto.code()).isNotBlank();
        assertThat(dto.leaderId()).isEqualTo("leader-1");
        assertThat(dto.members()).hasSize(1);
        assertThat(dto.members().getFirst().displayName()).isEqualTo("Leader");
    }

    @Test
    void joinPartyAddsMemberWithChosenDisplayNameAndBroadcastsState() {
        PartyStateDto created = partyService.createParty(leader, "Leader");

        PartyStateDto joined = partyService.joinParty(created.code(), member, "Player Two");

        assertThat(joined.members()).hasSize(2);
        assertThat(joined.members().get(1).displayName()).isEqualTo("Player Two");
        verify(messagingTemplate, times(1))
                .convertAndSend("/topic/party/" + created.code(), joined);
    }

    @Test
    void joinUnknownPartyThrows() {
        assertThatThrownBy(() -> partyService.joinParty("NOPE99", member, "Player Two"))
                .isInstanceOf(PartyNotFoundException.class);
    }

    @Test
    void selectClassPersistsChoice() {
        PartyStateDto created = partyService.createParty(leader, "Leader");

        PartyStateDto updated = partyService.selectClass(created.code(), leader, CharacterClass.HEALER);

        assertThat(updated.members().getFirst().characterClass()).isEqualTo(CharacterClass.HEALER);
    }

    @Test
    void selectClassAlreadyTakenByAnotherMemberThrows() {
        PartyStateDto created = partyService.createParty(leader, "Leader");
        partyService.joinParty(created.code(), member, "Player Two");
        partyService.selectClass(created.code(), leader, CharacterClass.MAGE);

        assertThatThrownBy(() -> partyService.selectClass(created.code(), member, CharacterClass.MAGE))
                .isInstanceOf(ClassAlreadyTakenException.class);
    }

    @Test
    void onlyLeaderCanStartGame() {
        PartyStateDto created = partyService.createParty(leader, "Leader");
        partyService.joinParty(created.code(), member, "Player Two");

        assertThatThrownBy(() -> partyService.startGame(created.code(), member, List.of("leader-1", "player-2")))
                .isInstanceOf(NotPartyLeaderException.class);
    }

    @Test
    void leaderStartsGameSuccessfully() {
        PartyStateDto created = partyService.createParty(leader, "Leader");
        partyService.joinParty(created.code(), member, "Player Two");
        partyService.selectClass(created.code(), leader, CharacterClass.WARRIOR);
        partyService.selectClass(created.code(), member, CharacterClass.HEALER);
        assertThat(created.status()).isEqualTo(PartyStatus.LOBBY);

        PartyStateDto updated = partyService.startGame(created.code(), leader, List.of("player-2", "leader-1"));

        assertThat(updated.turnOrder()).containsExactly("player-2", "leader-1");
        assertThat(updated.status()).isEqualTo(PartyStatus.IN_PROGRESS);
    }

    @Test
    void addFakeMembersAddsBotsFlaggedAsSuch() {
        PartyStateDto created = partyService.createParty(leader, "Leader");

        PartyStateDto updated = partyService.addFakeMembers(created.code(), 3);

        assertThat(updated.members()).hasSize(4);
        assertThat(updated.members().stream().filter(PartyMemberDto::bot)).hasSize(3);
        assertThat(updated.members().stream().filter(m -> !m.bot())).hasSize(1);
    }

    @Test
    void selectClassAsFakeMemberUpdatesTheBot() {
        PartyStateDto created = partyService.createParty(leader, "Leader");
        PartyStateDto withBots = partyService.addFakeMembers(created.code(), 1);
        String botId = withBots.members().stream().filter(PartyMemberDto::bot).findFirst().orElseThrow().userId();

        PartyStateDto updated = partyService.selectClassAsFakeMember(created.code(), botId, CharacterClass.MAGE);

        assertThat(updated.members().stream().filter(m -> m.userId().equals(botId)).findFirst().orElseThrow().characterClass())
                .isEqualTo(CharacterClass.MAGE);
    }

    @Test
    void selectClassAsFakeMemberRejectsRealMemberTarget() {
        PartyStateDto created = partyService.createParty(leader, "Leader");

        assertThatThrownBy(() -> partyService.selectClassAsFakeMember(created.code(), "leader-1", CharacterClass.MAGE))
                .isInstanceOf(NotAFakeMemberException.class);
    }
}
