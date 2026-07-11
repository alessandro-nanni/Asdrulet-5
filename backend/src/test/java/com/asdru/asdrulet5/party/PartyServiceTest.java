package com.asdru.asdrulet5.party;

import com.asdru.asdrulet5.auth.AuthenticatedUser;
import com.asdru.asdrulet5.party.domain.CharacterClass;
import com.asdru.asdrulet5.party.exception.NotPartyLeaderException;
import com.asdru.asdrulet5.party.exception.PartyNotFoundException;
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

    private final AuthenticatedUser leader = new AuthenticatedUser("leader-1", "Leader", "leader.png");
    private final AuthenticatedUser member = new AuthenticatedUser("player-2", "Player Two", "player2.png");

    private PartyService partyService;
    private SimpMessagingTemplate messagingTemplate;

    @BeforeEach
    void setUp() {
        messagingTemplate = mock(SimpMessagingTemplate.class);
        partyService = new PartyService(new InMemoryPartyRepository(), messagingTemplate);
    }

    @Test
    void createPartyReturnsCodeWithLeaderAsOnlyMember() {
        PartyStateDto dto = partyService.createParty(leader);

        assertThat(dto.code()).isNotBlank();
        assertThat(dto.leaderId()).isEqualTo("leader-1");
        assertThat(dto.members()).hasSize(1);
    }

    @Test
    void joinPartyAddsMemberAndBroadcastsState() {
        PartyStateDto created = partyService.createParty(leader);

        PartyStateDto joined = partyService.joinParty(created.code(), member);

        assertThat(joined.members()).hasSize(2);
        verify(messagingTemplate, times(1))
                .convertAndSend("/topic/party/" + created.code(), joined);
    }

    @Test
    void joinUnknownPartyThrows() {
        assertThatThrownBy(() -> partyService.joinParty("NOPE99", member))
                .isInstanceOf(PartyNotFoundException.class);
    }

    @Test
    void selectClassPersistsChoice() {
        PartyStateDto created = partyService.createParty(leader);

        PartyStateDto updated = partyService.selectClass(created.code(), leader, CharacterClass.HEALER);

        assertThat(updated.members().get(0).characterClass()).isEqualTo(CharacterClass.HEALER);
    }

    @Test
    void onlyLeaderCanSetTurnOrder() {
        PartyStateDto created = partyService.createParty(leader);
        partyService.joinParty(created.code(), member);

        assertThatThrownBy(() -> partyService.setTurnOrder(created.code(), member, List.of("leader-1", "player-2")))
                .isInstanceOf(NotPartyLeaderException.class);
    }

    @Test
    void leaderSetsTurnOrderSuccessfully() {
        PartyStateDto created = partyService.createParty(leader);
        partyService.joinParty(created.code(), member);

        PartyStateDto updated = partyService.setTurnOrder(created.code(), leader, List.of("player-2", "leader-1"));

        assertThat(updated.turnOrder()).containsExactly("player-2", "leader-1");
    }
}
