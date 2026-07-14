package com.asdru.asdrulet5.party;

import com.asdru.asdrulet5.auth.AuthenticatedUser;
import com.asdru.asdrulet5.combat.CombatService;
import com.asdru.asdrulet5.combat.CombatVictoryEvent;
import com.asdru.asdrulet5.dungeon.DungeonService;
import com.asdru.asdrulet5.dungeon.domain.RoomType;
import com.asdru.asdrulet5.inventory.ItemDefinitionRegistry;
import com.asdru.asdrulet5.inventory.exception.UnknownItemDefinitionException;
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
import static org.mockito.Mockito.*;

class PartyServiceTest {

    private final AuthenticatedUser leader = new AuthenticatedUser("leader-1", "Google Name", "leader.png");
    private final AuthenticatedUser member = new AuthenticatedUser("player-2", "Google Name Two", "player2.png");

    private PartyService partyService;
    private SimpMessagingTemplate messagingTemplate;
    private DungeonService dungeonService;
    private CombatService combatService;

    @BeforeEach
    void setUp() {
        messagingTemplate = mock(SimpMessagingTemplate.class);
        dungeonService = mock(DungeonService.class);
        combatService = mock(CombatService.class);
        partyService = new PartyService(new InMemoryPartyRepository(), messagingTemplate, dungeonService,
                combatService, new ItemDefinitionRegistry());
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
        assertThat(updated.status()).isEqualTo(PartyStatus.DUNGEON);
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

    @Test
    void enterRoomByNonLeaderThrows() {
        PartyStateDto created = partyService.createParty(leader, "Leader");
        partyService.joinParty(created.code(), member, "Player Two");
        when(dungeonService.enterNode(created.code(), "player-2")).thenReturn(RoomType.FIGHT);

        assertThatThrownBy(() -> partyService.enterRoom(created.code(), "player-2"))
                .isInstanceOf(NotPartyLeaderException.class);
    }

    @Test
    void enterRoomInLootRoomAutoClearsWithoutStartingCombat() {
        PartyStateDto created = partyService.createParty(leader, "Leader");
        when(dungeonService.enterNode(created.code(), "leader-1")).thenReturn(RoomType.LOOT);

        PartyStateDto updated = partyService.enterRoom(created.code(), "leader-1");

        assertThat(updated.status()).isEqualTo(PartyStatus.LOBBY);
        verify(dungeonService, times(1)).clearEnteredNode(created.code());
        verifyNoInteractions(combatService);
    }

    @Test
    void enterRoomByLeaderInFightRoomStartsCombatAndSetsStatus() {
        PartyStateDto created = partyService.createParty(leader, "Leader");
        when(dungeonService.enterNode(created.code(), "leader-1")).thenReturn(RoomType.FIGHT);

        PartyStateDto updated = partyService.enterRoom(created.code(), "leader-1");

        assertThat(updated.status()).isEqualTo(PartyStatus.IN_PROGRESS);
        verify(combatService, times(1)).startCombat(eq(created.code()), any(), any());
    }

    @Test
    void enterRoomInBossRoomIsAllowed() {
        PartyStateDto created = partyService.createParty(leader, "Leader");
        when(dungeonService.enterNode(created.code(), "leader-1")).thenReturn(RoomType.BOSS);

        PartyStateDto updated = partyService.enterRoom(created.code(), "leader-1");

        assertThat(updated.status()).isEqualTo(PartyStatus.IN_PROGRESS);
    }

    @Test
    void combatVictoryEventReturnsPartyToDungeonAndClearsTheNode() {
        PartyStateDto created = partyService.createParty(leader, "Leader");
        when(dungeonService.enterNode(created.code(), "leader-1")).thenReturn(RoomType.FIGHT);
        partyService.enterRoom(created.code(), "leader-1");

        partyService.onCombatVictory(new CombatVictoryEvent(created.code()));

        verify(dungeonService, times(1)).clearEnteredNode(created.code());
        assertThat(partyService.getState(created.code()).status()).isEqualTo(PartyStatus.DUNGEON);
    }

    @Test
    void equipItemFillsTheMatchingSlot() {
        PartyStateDto created = partyService.createParty(leader, "Leader");

        PartyStateDto updated = partyService.equipItem(created.code(), "leader-1", "rusted-sword");

        assertThat(updated.members().getFirst().loadout().weaponItemId()).isEqualTo("rusted-sword");
        assertThat(updated.members().getFirst().loadout().chestplateItemId()).isNull();
    }

    @Test
    void equipItemReplacesWhateverWasInThatSlotBefore() {
        PartyStateDto created = partyService.createParty(leader, "Leader");
        partyService.equipItem(created.code(), "leader-1", "rusted-sword");

        PartyStateDto updated = partyService.equipItem(created.code(), "leader-1", "flame-edge");

        assertThat(updated.members().getFirst().loadout().weaponItemId()).isEqualTo("flame-edge");
    }

    @Test
    void equipUnknownItemThrows() {
        PartyStateDto created = partyService.createParty(leader, "Leader");

        assertThatThrownBy(() -> partyService.equipItem(created.code(), "leader-1", "no-such-item"))
                .isInstanceOf(UnknownItemDefinitionException.class);
    }
}
