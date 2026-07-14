package com.asdru.asdrulet5.party;

import com.asdru.asdrulet5.auth.AuthenticatedUser;
import com.asdru.asdrulet5.combat.CombatService;
import com.asdru.asdrulet5.combat.CombatVictoryEvent;
import com.asdru.asdrulet5.dungeon.DungeonService;
import com.asdru.asdrulet5.dungeon.domain.RoomType;
import com.asdru.asdrulet5.inventory.ItemDefinitionRegistry;
import com.asdru.asdrulet5.inventory.domain.ItemDefinition;
import com.asdru.asdrulet5.inventory.exception.UnknownItemDefinitionException;
import com.asdru.asdrulet5.party.domain.CharacterClass;
import com.asdru.asdrulet5.party.domain.Party;
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
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class PartyServiceTest {

    private final AuthenticatedUser leader = new AuthenticatedUser("leader-1", "Leader", "leader.png");
    private final AuthenticatedUser member = new AuthenticatedUser("player-2", "Player Two", "player2.png");

    private PartyService partyService;
    private SimpMessagingTemplate messagingTemplate;
    private DungeonService dungeonService;
    private CombatService combatService;
    private ScheduledExecutorService victoryReturnScheduler;
    private RoomEntryDelay roomEntryDelay;

    @BeforeEach
    void setUp() {
        messagingTemplate = mock(SimpMessagingTemplate.class);
        dungeonService = mock(DungeonService.class);
        combatService = mock(CombatService.class);
        victoryReturnScheduler = mock(ScheduledExecutorService.class);
        // Runs the scheduled task immediately rather than actually waiting —
        // keeps tests fast/deterministic while still letting us assert on
        // the delay/unit the real code schedules with.
        doAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            task.run();
            return null;
        }).when(victoryReturnScheduler).schedule(any(Runnable.class), anyLong(), any(TimeUnit.class));
        // A real RoomEntryDelay would block each enterRoom call for real —
        // pointless in a unit test, so use one configured to sleep 0ms.
        roomEntryDelay = new RoomEntryDelay(0);
        partyService = new PartyService(new InMemoryPartyRepository(), messagingTemplate, dungeonService,
                combatService, new ItemDefinitionRegistry(), victoryReturnScheduler, roomEntryDelay);
    }

    @Test
    void createPartyReturnsCodeWithLeaderAsOnlyMember() {
        PartyStateDto dto = partyService.createParty(leader);

        assertThat(dto.code()).isNotBlank();
        assertThat(dto.leaderId()).isEqualTo("leader-1");
        assertThat(dto.members()).hasSize(1);
        assertThat(dto.members().getFirst().displayName()).isEqualTo("Leader");
    }

    @Test
    void joinPartyAddsMemberWithChosenDisplayNameAndBroadcastsState() {
        PartyStateDto created = partyService.createParty(leader);

        PartyStateDto joined = partyService.joinParty(created.code(), member);

        assertThat(joined.members()).hasSize(2);
        assertThat(joined.members().get(1).displayName()).isEqualTo("Player Two");
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

        PartyStateDto updated = partyService.selectClass(created.code(), "leader-1", CharacterClass.HEALER);

        assertThat(updated.members().getFirst().characterClass()).isEqualTo(CharacterClass.HEALER);
    }

    @Test
    void selectClassAlreadyTakenByAnotherMemberThrows() {
        PartyStateDto created = partyService.createParty(leader);
        partyService.joinParty(created.code(), member);
        partyService.selectClass(created.code(), "leader-1", CharacterClass.MAGE);

        assertThatThrownBy(() -> partyService.selectClass(created.code(), "player-2", CharacterClass.MAGE))
                .isInstanceOf(ClassAlreadyTakenException.class);
    }

    @Test
    void onlyLeaderCanStartGame() {
        PartyStateDto created = partyService.createParty(leader);
        partyService.joinParty(created.code(), member);

        assertThatThrownBy(() -> partyService.startGame(created.code(), "player-2", List.of("leader-1", "player-2")))
                .isInstanceOf(NotPartyLeaderException.class);
    }

    @Test
    void leaderStartsGameSuccessfully() {
        PartyStateDto created = partyService.createParty(leader);
        partyService.joinParty(created.code(), member);
        partyService.selectClass(created.code(), "leader-1", CharacterClass.WARRIOR);
        partyService.selectClass(created.code(), "player-2", CharacterClass.HEALER);
        assertThat(created.status()).isEqualTo(PartyStatus.LOBBY);

        PartyStateDto updated = partyService.startGame(created.code(), "leader-1", List.of("player-2", "leader-1"));

        assertThat(updated.turnOrder()).containsExactly("player-2", "leader-1");
        assertThat(updated.status()).isEqualTo(PartyStatus.DUNGEON);
    }

    @Test
    void addFakeMembersAddsBotsFlaggedAsSuch() {
        PartyStateDto created = partyService.createParty(leader);

        PartyStateDto updated = partyService.addFakeMembers(created.code(), 3);

        assertThat(updated.members()).hasSize(4);
        assertThat(updated.members().stream().filter(PartyMemberDto::bot)).hasSize(3);
        assertThat(updated.members().stream().filter(m -> !m.bot())).hasSize(1);
    }

    @Test
    void selectClassAsFakeMemberUpdatesTheBot() {
        PartyStateDto created = partyService.createParty(leader);
        PartyStateDto withBots = partyService.addFakeMembers(created.code(), 1);
        String botId = withBots.members().stream().filter(PartyMemberDto::bot).findFirst().orElseThrow().userId();

        PartyStateDto updated = partyService.selectClassAsFakeMember(created.code(), botId, CharacterClass.MAGE);

        assertThat(updated.members().stream().filter(m -> m.userId().equals(botId)).findFirst().orElseThrow().characterClass())
                .isEqualTo(CharacterClass.MAGE);
    }

    @Test
    void selectClassAsFakeMemberRejectsRealMemberTarget() {
        PartyStateDto created = partyService.createParty(leader);

        assertThatThrownBy(() -> partyService.selectClassAsFakeMember(created.code(), "leader-1", CharacterClass.MAGE))
                .isInstanceOf(NotAFakeMemberException.class);
    }

    @Test
    void enterRoomByNonLeaderThrows() {
        PartyStateDto created = partyService.createParty(leader);
        partyService.joinParty(created.code(), member);
        when(dungeonService.enterNode(created.code(), "player-2")).thenReturn(RoomType.FIGHT);

        assertThatThrownBy(() -> partyService.enterRoom(created.code(), "player-2"))
                .isInstanceOf(NotPartyLeaderException.class);
    }

    @Test
    void enterRoomInLootRoomAutoClearsWithoutStartingCombat() {
        PartyStateDto created = partyService.createParty(leader);
        when(dungeonService.enterNode(created.code(), "leader-1")).thenReturn(RoomType.LOOT);

        PartyStateDto updated = partyService.enterRoom(created.code(), "leader-1");

        assertThat(updated.status()).isEqualTo(PartyStatus.LOBBY);
        verify(dungeonService, times(1)).clearEnteredNode(created.code());
        verifyNoInteractions(combatService);
    }

    @Test
    void enterRoomByLeaderInFightRoomStartsCombatAndSetsStatus() {
        PartyStateDto created = partyService.createParty(leader);
        when(dungeonService.enterNode(created.code(), "leader-1")).thenReturn(RoomType.FIGHT);

        PartyStateDto updated = partyService.enterRoom(created.code(), "leader-1");

        assertThat(updated.status()).isEqualTo(PartyStatus.IN_PROGRESS);
        verify(combatService, times(1)).startCombat(eq(created.code()), any(), any());
    }

    @Test
    void enterRoomInBossRoomIsAllowed() {
        PartyStateDto created = partyService.createParty(leader);
        when(dungeonService.enterNode(created.code(), "leader-1")).thenReturn(RoomType.BOSS);

        PartyStateDto updated = partyService.enterRoom(created.code(), "leader-1");

        assertThat(updated.status()).isEqualTo(PartyStatus.IN_PROGRESS);
    }

    @Test
    void combatVictoryEventReturnsPartyToDungeonAndClearsTheNode() {
        PartyStateDto created = partyService.createParty(leader);
        when(dungeonService.enterNode(created.code(), "leader-1")).thenReturn(RoomType.FIGHT);
        partyService.enterRoom(created.code(), "leader-1");

        partyService.onCombatVictory(new CombatVictoryEvent(created.code()));

        verify(victoryReturnScheduler).schedule(any(Runnable.class), eq(3L), eq(TimeUnit.SECONDS));
        verify(dungeonService, times(1)).clearEnteredNode(created.code());
        assertThat(partyService.getState(created.code()).status()).isEqualTo(PartyStatus.DUNGEON);
    }

    @Test
    void equipItemFillsTheMatchingSlot() {
        PartyStateDto created = partyService.createParty(leader);

        PartyStateDto updated = partyService.equipItem(created.code(), "leader-1", "rusted-sword");

        assertThat(updated.members().getFirst().loadout().weaponItemId()).isEqualTo("rusted-sword");
        assertThat(updated.members().getFirst().loadout().chestplateItemId()).isNull();
    }

    @Test
    void equipItemReplacesWhateverWasInThatSlotBefore() {
        PartyStateDto created = partyService.createParty(leader);
        partyService.equipItem(created.code(), "leader-1", "rusted-sword");

        PartyStateDto updated = partyService.equipItem(created.code(), "leader-1", "flame-edge");

        assertThat(updated.members().getFirst().loadout().weaponItemId()).isEqualTo("flame-edge");
    }

    @Test
    void equipUnknownItemThrows() {
        PartyStateDto created = partyService.createParty(leader);

        assertThatThrownBy(() -> partyService.equipItem(created.code(), "leader-1", "no-such-item"))
                .isInstanceOf(UnknownItemDefinitionException.class);
    }

    @Test
    void createPartySeedsSharedStorageWithFullCatalog() {
        PartyStateDto created = partyService.createParty(leader);

        assertThat(created.storage()).hasSize(Party.STORAGE_SIZE);
        assertThat(created.storage()).filteredOn(java.util.Objects::nonNull)
                .containsExactlyInAnyOrderElementsOf(
                        new ItemDefinitionRegistry().all().stream().map(ItemDefinition::id).toList());
    }

    @Test
    void equipFromStorageEquipsItemAndSwapsPreviousBackIntoTheSameCell() {
        PartyStateDto created = partyService.createParty(leader);
        int swordIndex = created.storage().indexOf("rusted-sword");
        int flameIndex = created.storage().indexOf("flame-edge");

        PartyStateDto afterFirstEquip = partyService.equipFromStorage(created.code(), "leader-1", swordIndex);
        assertThat(afterFirstEquip.members().getFirst().loadout().weaponItemId()).isEqualTo("rusted-sword");
        assertThat(afterFirstEquip.storage().get(swordIndex)).isNull();

        PartyStateDto afterSecondEquip = partyService.equipFromStorage(created.code(), "leader-1", flameIndex);

        assertThat(afterSecondEquip.members().getFirst().loadout().weaponItemId()).isEqualTo("flame-edge");
        // The previously-equipped rusted sword goes back into the cell the
        // new item just vacated, so nothing is lost or duplicated.
        assertThat(afterSecondEquip.storage().get(flameIndex)).isEqualTo("rusted-sword");
    }

    @Test
    void equipFromEmptyStorageCellThrows() {
        PartyStateDto created = partyService.createParty(leader);
        int emptyIndex = created.storage().indexOf(null);

        assertThatThrownBy(() -> partyService.equipFromStorage(created.code(), "leader-1", emptyIndex))
                .isInstanceOf(com.asdru.asdrulet5.party.exception.EmptyStorageSlotException.class);
    }

    @Test
    void equipFromOutOfRangeStorageIndexThrows() {
        PartyStateDto created = partyService.createParty(leader);

        assertThatThrownBy(() -> partyService.equipFromStorage(created.code(), "leader-1", Party.STORAGE_SIZE))
                .isInstanceOf(com.asdru.asdrulet5.party.exception.InvalidStorageIndexException.class);
    }
}
