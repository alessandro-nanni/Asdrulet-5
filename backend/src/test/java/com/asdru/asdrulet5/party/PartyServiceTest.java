package com.asdru.asdrulet5.party;

import com.asdru.asdrulet5.auth.AuthenticatedUser;
import com.asdru.asdrulet5.classdata.ClassDefinitionRegistry;
import com.asdru.asdrulet5.classdata.domain.ActiveEffect;
import com.asdru.asdrulet5.classdata.domain.Damage;
import com.asdru.asdrulet5.classdata.domain.Stats;
import com.asdru.asdrulet5.combat.CombatService;
import com.asdru.asdrulet5.combat.CombatVictoryEvent;
import com.asdru.asdrulet5.combat.domain.Combatant;
import com.asdru.asdrulet5.combat.domain.PlayerCombatant;
import com.asdru.asdrulet5.dungeon.DungeonService;
import com.asdru.asdrulet5.dungeon.domain.RoomType;
import com.asdru.asdrulet5.inventory.ItemDefinitionRegistry;
import com.asdru.asdrulet5.inventory.LootTableRegistry;
import com.asdru.asdrulet5.inventory.exception.UnknownItemDefinitionException;
import com.asdru.asdrulet5.party.domain.CharacterClass;
import com.asdru.asdrulet5.party.domain.Party;
import com.asdru.asdrulet5.party.domain.PartyStatus;
import com.asdru.asdrulet5.party.domain.WheelEffect;
import com.asdru.asdrulet5.party.exception.*;
import com.asdru.asdrulet5.party.web.dto.LootResultDto;
import com.asdru.asdrulet5.party.web.dto.PartyMemberDto;
import com.asdru.asdrulet5.party.web.dto.PartyStateDto;
import com.asdru.asdrulet5.party.web.dto.PendingEffectDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PartyServiceTest {

    private final AuthenticatedUser leader = new AuthenticatedUser("leader-1", "Leader", "leader.png");
    private final AuthenticatedUser member = new AuthenticatedUser("player-2", "Player Two", "player2.png");

    private PartyService partyService;
    private PartyRepository partyRepository;
    private SimpMessagingTemplate messagingTemplate;
    private DungeonService dungeonService;
    private CombatService combatService;
    private ScheduledExecutorService victoryReturnScheduler;
    private RoomEntryDelay roomEntryDelay;

    private static Combatant playerCombatant(String userId, int maxHealth) {
        return new PlayerCombatant(userId, userId, CharacterClass.BERSERKER,
                new Stats(maxHealth, 5, 100), 40, List.of(), List.of(), null);
    }

    @BeforeEach
    void setUp() {
        partyRepository = new InMemoryPartyRepository();
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
        partyService = new PartyService(partyRepository, messagingTemplate, dungeonService,
                combatService, new ItemDefinitionRegistry(), new LootTableRegistry(), new ClassDefinitionRegistry(false),
                victoryReturnScheduler, roomEntryDelay);
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
        partyService.selectClass(created.code(), "leader-1", CharacterClass.BERSERKER);
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
    void enterRoomInLootRoomDoesNotAutoClearOrStartCombat() {
        // Same as MYSTERY/MERCHANT — a real chest to open, not an
        // instant-clear flavor room; clearing is driven entirely by
        // acknowledgeLootResult (see below).
        PartyStateDto created = partyService.createParty(leader);
        when(dungeonService.enterNode(created.code(), "leader-1")).thenReturn(RoomType.LOOT);

        PartyStateDto updated = partyService.enterRoom(created.code(), "leader-1");

        assertThat(updated.status()).isEqualTo(PartyStatus.LOBBY);
        verify(dungeonService, never()).clearEnteredNode(created.code());
        verifyNoInteractions(combatService);
    }

    @Test
    void enterRoomByLeaderInFightRoomStartsCombatAndSetsStatus() {
        PartyStateDto created = partyService.createParty(leader);
        when(dungeonService.enterNode(created.code(), "leader-1")).thenReturn(RoomType.FIGHT);

        PartyStateDto updated = partyService.enterRoom(created.code(), "leader-1");

        assertThat(updated.status()).isEqualTo(PartyStatus.IN_PROGRESS);
        // false: a regular FIGHT room draws from the enemy pool, not the boss.
        verify(combatService, times(1)).startCombat(argThat(party -> party.code().equals(created.code())), any(), any(), eq(false));
    }

    @Test
    void enterRoomInBossRoomIsAllowed() {
        PartyStateDto created = partyService.createParty(leader);
        when(dungeonService.enterNode(created.code(), "leader-1")).thenReturn(RoomType.BOSS);

        PartyStateDto updated = partyService.enterRoom(created.code(), "leader-1");

        assertThat(updated.status()).isEqualTo(PartyStatus.IN_PROGRESS);
        verify(combatService, times(1)).startCombat(argThat(party -> party.code().equals(created.code())), any(), any(), eq(true));
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
    void combatVictoryCarriesTheFightsEndingDamageOntoTheMember() {
        // A fight's own outcome must survive into the next room — a member
        // who took damage shouldn't show back up at full health the moment
        // the party returns to the dungeon.
        PartyStateDto created = partyService.createParty(leader);
        Combatant combatant = playerCombatant("leader-1", 100);
        combatant.applyDamage(Damage.of(30));
        when(combatService.partyCombatantsFor(created.code())).thenReturn(List.of(combatant));

        partyService.onCombatVictory(new CombatVictoryEvent(created.code()));

        assertThat(partyService.getState(created.code()).members().getFirst().currentHealth()).isEqualTo(70);
    }

    @Test
    void combatVictoryNormalizesFullEndingHealthToNull() {
        // Matches the "null means full health" convention used everywhere
        // else this field is set (e.g. the wheel's FULL_HEAL).
        PartyStateDto created = partyService.createParty(leader);
        Combatant combatant = playerCombatant("leader-1", 100);
        when(combatService.partyCombatantsFor(created.code())).thenReturn(List.of(combatant));

        partyService.onCombatVictory(new CombatVictoryEvent(created.code()));

        assertThat(partyService.getState(created.code()).members().getFirst().currentHealth()).isNull();
    }

    @Test
    void combatVictoryCarriesStillActiveEffectsIntoPendingEffects() {
        // A buff/DoT that hadn't expired yet when the fight ended should
        // still be waiting for the member going into their next fight —
        // exactly like a MYSTERY wheel's poison result would be.
        PartyStateDto created = partyService.createParty(leader);
        Combatant combatant = playerCombatant("leader-1", 100);
        combatant.addActiveEffect(ActiveEffect.defenseBuff("Taunt", "shield", 10, 2));
        when(combatService.partyCombatantsFor(created.code())).thenReturn(List.of(combatant));

        partyService.onCombatVictory(new CombatVictoryEvent(created.code()));

        List<PendingEffectDto> pending = partyService.getState(created.code()).members().getFirst().pendingEffects();
        assertThat(pending).hasSize(1);
        assertThat(pending.getFirst().name()).isEqualTo("Taunt");
        assertThat(pending.getFirst().remainingTurns()).isEqualTo(2);
    }

    @Test
    void equipItemFillsTheMatchingSlot() {
        PartyStateDto created = partyService.createParty(leader);

        PartyStateDto updated = partyService.equipItem(created.code(), "leader-1", "scythe");

        assertThat(updated.members().getFirst().loadout().weaponItemId()).isEqualTo("scythe");
        assertThat(updated.members().getFirst().loadout().chestplateItemId()).isNull();
    }

    @Test
    void equipItemReplacesWhateverWasInThatSlotBefore() {
        PartyStateDto created = partyService.createParty(leader);
        partyService.equipItem(created.code(), "leader-1", "scythe");

        PartyStateDto updated = partyService.equipItem(created.code(), "leader-1", "torch");

        assertThat(updated.members().getFirst().loadout().weaponItemId()).isEqualTo("torch");
    }

    @Test
    void equipUnknownItemThrows() {
        PartyStateDto created = partyService.createParty(leader);

        assertThatThrownBy(() -> partyService.equipItem(created.code(), "leader-1", "no-such-item"))
                .isInstanceOf(UnknownItemDefinitionException.class);
    }

    @Test
    void createPartyStartsWithEmptyStorage() {
        PartyStateDto created = partyService.createParty(leader);

        assertThat(created.storage()).hasSize(Party.STORAGE_SIZE);
        assertThat(created.storage()).allMatch(java.util.Objects::isNull);
    }

    @Test
    void equipFromStorageEquipsItemAndSwapsPreviousBackIntoTheSameCell() {
        PartyStateDto created = partyService.createParty(leader);
        Party party = partyRepository.findByCode(created.code()).orElseThrow();
        party.seedStorage(List.of("scythe", "torch"));
        int swordIndex = 0;
        int flameIndex = 1;

        PartyStateDto afterFirstEquip = partyService.equipFromStorage(created.code(), "leader-1", swordIndex);
        assertThat(afterFirstEquip.members().getFirst().loadout().weaponItemId()).isEqualTo("scythe");
        assertThat(afterFirstEquip.storage().get(swordIndex)).isNull();

        PartyStateDto afterSecondEquip = partyService.equipFromStorage(created.code(), "leader-1", flameIndex);

        assertThat(afterSecondEquip.members().getFirst().loadout().weaponItemId()).isEqualTo("torch");
        // The previously-equipped scythe goes back into the cell the
        // new item just vacated, so nothing is lost or duplicated.
        assertThat(afterSecondEquip.storage().get(flameIndex)).isEqualTo("scythe");
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

    @Test
    void enterRoomInMysteryRoomDoesNotAutoClear() {
        PartyStateDto created = partyService.createParty(leader);
        when(dungeonService.enterNode(created.code(), "leader-1")).thenReturn(RoomType.MYSTERY);

        partyService.enterRoom(created.code(), "leader-1");

        verify(dungeonService, never()).clearEnteredNode(created.code());
        verifyNoInteractions(combatService);
    }

    @Test
    void spinWheelWhenNoRoomIsEnteredThrows() {
        PartyStateDto created = partyService.createParty(leader);

        assertThatThrownBy(() -> partyService.spinWheel(created.code(), "leader-1"))
                .isInstanceOf(com.asdru.asdrulet5.party.exception.NotInMysteryRoomException.class);
    }

    @Test
    void spinWheelByUnknownMemberThrows() {
        PartyStateDto created = partyService.createParty(leader);

        assertThatThrownBy(() -> partyService.spinWheel(created.code(), "nobody"))
                .isInstanceOf(NotPartyMemberException.class);
    }

    @Test
    void spinWheelTwiceInTheSameRoomThrows() {
        PartyStateDto created = partyService.createParty(leader);
        partyService.selectClass(created.code(), "leader-1", CharacterClass.BERSERKER);
        partyService.startGame(created.code(), "leader-1", List.of("leader-1"));
        when(dungeonService.enteredRoomType(created.code())).thenReturn(RoomType.MYSTERY);

        partyService.spinWheel(created.code(), "leader-1");

        assertThatThrownBy(() -> partyService.spinWheel(created.code(), "leader-1"))
                .isInstanceOf(com.asdru.asdrulet5.party.exception.AlreadySpunWheelException.class);
    }

    @Test
    void spinWheelOutOfTurnOrderThrows() {
        PartyStateDto created = partyService.createParty(leader);
        partyService.joinParty(created.code(), member);
        partyService.selectClass(created.code(), "leader-1", CharacterClass.BERSERKER);
        partyService.selectClass(created.code(), "player-2", CharacterClass.HEALER);
        partyService.startGame(created.code(), "leader-1", List.of("leader-1", "player-2"));
        when(dungeonService.enteredRoomType(created.code())).thenReturn(RoomType.MYSTERY);

        assertThatThrownBy(() -> partyService.spinWheel(created.code(), "player-2"))
                .isInstanceOf(com.asdru.asdrulet5.party.exception.NotYourWheelTurnException.class);
    }

    @Test
    void spinWheelInTurnOrderSucceedsForEachMemberInSequence() {
        PartyStateDto created = partyService.createParty(leader);
        partyService.joinParty(created.code(), member);
        partyService.selectClass(created.code(), "leader-1", CharacterClass.BERSERKER);
        partyService.selectClass(created.code(), "player-2", CharacterClass.HEALER);
        // Deliberately not join order, to prove this is genuinely turnOrder-driven.
        partyService.startGame(created.code(), "leader-1", List.of("player-2", "leader-1"));
        when(dungeonService.enteredRoomType(created.code())).thenReturn(RoomType.MYSTERY);

        assertThatThrownBy(() -> partyService.spinWheel(created.code(), "leader-1"))
                .isInstanceOf(com.asdru.asdrulet5.party.exception.NotYourWheelTurnException.class);

        assertThatCode(() -> partyService.spinWheel(created.code(), "player-2")).doesNotThrowAnyException();
        assertThatCode(() -> partyService.spinWheel(created.code(), "leader-1")).doesNotThrowAnyException();
    }

    @Test
    void spinWheelNeverRepeatsAnEffectAcrossDifferentMembers() {
        PartyStateDto created = partyService.createParty(leader);
        partyService.joinParty(created.code(), member);
        AuthenticatedUser member3 = new AuthenticatedUser("player-3", "Player Three", "p3.png");
        AuthenticatedUser member4 = new AuthenticatedUser("player-4", "Player Four", "p4.png");
        partyService.joinParty(created.code(), member3);
        partyService.joinParty(created.code(), member4);
        partyService.selectClass(created.code(), "leader-1", CharacterClass.BERSERKER);
        partyService.selectClass(created.code(), "player-2", CharacterClass.HEALER);
        partyService.selectClass(created.code(), "player-3", CharacterClass.PALADIN);
        partyService.selectClass(created.code(), "player-4", CharacterClass.MAGE);
        List<String> order = List.of("leader-1", "player-2", "player-3", "player-4");
        partyService.startGame(created.code(), "leader-1", order);
        when(dungeonService.enteredRoomType(created.code())).thenReturn(RoomType.MYSTERY);

        for (String userId : order) {
            partyService.spinWheel(created.code(), userId);
        }

        PartyStateDto finalState = partyService.getState(created.code());
        assertThat(finalState.wheelResults()).hasSize(4);
        assertThat(finalState.wheelResults().values().stream().toList()).doesNotHaveDuplicates();
    }

    @Test
    void spinWheelNeverClearsTheRoomByItself() {
        // Clearing is driven entirely by acknowledgeWheelResult now (see
        // below) — spinning alone, even by every member, must never clear
        // the room on its own.
        PartyStateDto created = partyService.createParty(leader);
        partyService.joinParty(created.code(), member);
        partyService.selectClass(created.code(), "leader-1", CharacterClass.BERSERKER);
        partyService.selectClass(created.code(), "player-2", CharacterClass.HEALER);
        partyService.startGame(created.code(), "leader-1", List.of("leader-1", "player-2"));
        when(dungeonService.enteredRoomType(created.code())).thenReturn(RoomType.MYSTERY);

        partyService.spinWheel(created.code(), "leader-1");
        partyService.spinWheel(created.code(), "player-2");

        verify(dungeonService, never()).clearEnteredNode(created.code());
    }

    @Test
    void acknowledgeBeforeSpinningThrows() {
        PartyStateDto created = partyService.createParty(leader);

        assertThatThrownBy(() -> partyService.acknowledgeWheelResult(created.code(), "leader-1"))
                .isInstanceOf(com.asdru.asdrulet5.party.exception.NotYetSpunWheelException.class);
    }

    @Test
    void acknowledgeByUnknownMemberThrows() {
        PartyStateDto created = partyService.createParty(leader);

        assertThatThrownBy(() -> partyService.acknowledgeWheelResult(created.code(), "nobody"))
                .isInstanceOf(NotPartyMemberException.class);
    }

    @Test
    void acknowledgeClearsTheRoomOnlyOnceEveryMemberHasAcknowledged() {
        PartyStateDto created = partyService.createParty(leader);
        partyService.joinParty(created.code(), member);
        partyService.selectClass(created.code(), "leader-1", CharacterClass.BERSERKER);
        partyService.selectClass(created.code(), "player-2", CharacterClass.HEALER);
        partyService.startGame(created.code(), "leader-1", List.of("leader-1", "player-2"));
        when(dungeonService.enteredRoomType(created.code())).thenReturn(RoomType.MYSTERY);
        partyService.spinWheel(created.code(), "leader-1");
        partyService.spinWheel(created.code(), "player-2");

        partyService.acknowledgeWheelResult(created.code(), "leader-1");
        verify(dungeonService, never()).clearEnteredNode(created.code());

        partyService.acknowledgeWheelResult(created.code(), "player-2");
        verify(dungeonService, times(1)).clearEnteredNode(created.code());
    }

    @Test
    void acknowledgeSoloClearsImmediately() {
        PartyStateDto created = partyService.createParty(leader);
        partyService.selectClass(created.code(), "leader-1", CharacterClass.BERSERKER);
        partyService.startGame(created.code(), "leader-1", List.of("leader-1"));
        when(dungeonService.enteredRoomType(created.code())).thenReturn(RoomType.MYSTERY);
        partyService.spinWheel(created.code(), "leader-1");

        partyService.acknowledgeWheelResult(created.code(), "leader-1");

        verify(dungeonService, times(1)).clearEnteredNode(created.code());
    }

    /**
     * The actual roll is randomized (see PartyService's SecureRandom field —
     * deliberately not injectable, same as DungeonGenerator's), so this
     * drives enough independent spins to hit every WheelEffect at least once
     * and checks each roll's own mutation matches what that effect promises,
     * rather than asserting on one specific outcome.
     */
    @Test
    void spinWheelAppliesWhicheverEffectItRolls() {
        java.util.Set<WheelEffect> observed = new java.util.HashSet<>();
        for (int i = 0; i < 150; i++) {
            PartyStateDto created = partyService.createParty(
                    new AuthenticatedUser("solo-" + i, "Solo", "avatar.png"));
            when(dungeonService.enteredRoomType(created.code())).thenReturn(RoomType.MYSTERY);
            partyService.selectClass(created.code(), "solo-" + i, CharacterClass.BERSERKER);
            partyService.startGame(created.code(), "solo-" + i, List.of("solo-" + i));

            PartyStateDto updated = partyService.spinWheel(created.code(), "solo-" + i);

            WheelEffect effect = updated.wheelResults().get("solo-" + i);
            observed.add(effect);
            PartyMemberDto self = updated.members().getFirst();
            switch (effect) {
                case FULL_HEAL -> assertThat(self.currentHealth()).isNull();
                case HALVE_HEALTH -> assertThat(self.currentHealth()).isNotNull().isPositive();
                case CLEAR_EFFECTS -> assertThat(self.pendingEffects()).isEmpty();
                case POISON -> {
                    assertThat(self.pendingEffects()).hasSize(1);
                    assertThat(self.pendingEffects().getFirst().name()).isEqualTo("Poison");
                    assertThat(self.pendingEffects().getFirst().remainingTurns()).isEqualTo(4);
                }
                case GIVE_ITEM -> {
                    // A personal reward equipped directly onto the spinner —
                    // never the shared storage grid (see Party.giveAndEquipItem).
                    boolean hasNewGear = self.loadout().weaponItemId() != null
                            || self.loadout().chestplateItemId() != null
                            || self.loadout().trinketItemId() != null;
                    assertThat(hasNewGear).isTrue();
                    assertThat(updated.storage()).allMatch(java.util.Objects::isNull);
                }
                case GIVE_COINS -> assertThat(updated.coins()).isEqualTo(30);
            }
        }
        assertThat(observed).containsExactlyInAnyOrder(WheelEffect.values());
    }

    @Test
    void spinWheelGiveItemWorksEvenWhenStorageIsAlreadyFull() {
        // GIVE_ITEM no longer needs shared-storage room (it equips onto the
        // spinner directly), so a full grid must never block or fail it.
        for (int i = 0; i < 30; i++) {
            PartyStateDto created = partyService.createParty(
                    new AuthenticatedUser("solo-" + i, "Solo", "avatar.png"));
            Party party = partyRepository.findByCode(created.code()).orElseThrow();
            List<String> full = new java.util.ArrayList<>();
            for (int cell = 0; cell < Party.STORAGE_SIZE; cell++) {
                full.add("scythe");
            }
            party.seedStorage(full);
            when(dungeonService.enteredRoomType(created.code())).thenReturn(RoomType.MYSTERY);
            String userId = "solo-" + i;
            partyService.selectClass(created.code(), userId, CharacterClass.BERSERKER);
            partyService.startGame(created.code(), userId, List.of(userId));

            assertThatCode(() -> partyService.spinWheel(created.code(), userId)).doesNotThrowAnyException();
        }
    }

    @Test
    void lootChestWhenNoRoomIsEnteredThrows() {
        PartyStateDto created = partyService.createParty(leader);

        assertThatThrownBy(() -> partyService.lootChest(created.code(), "leader-1"))
                .isInstanceOf(com.asdru.asdrulet5.party.exception.NotInLootRoomException.class);
    }

    @Test
    void lootChestByUnknownMemberThrows() {
        PartyStateDto created = partyService.createParty(leader);

        assertThatThrownBy(() -> partyService.lootChest(created.code(), "nobody"))
                .isInstanceOf(NotPartyMemberException.class);
    }

    @Test
    void lootChestTwiceInTheSameRoomThrows() {
        PartyStateDto created = partyService.createParty(leader);
        partyService.selectClass(created.code(), "leader-1", CharacterClass.BERSERKER);
        partyService.startGame(created.code(), "leader-1", List.of("leader-1"));
        when(dungeonService.enteredRoomType(created.code())).thenReturn(RoomType.LOOT);

        partyService.lootChest(created.code(), "leader-1");

        assertThatThrownBy(() -> partyService.lootChest(created.code(), "leader-1"))
                .isInstanceOf(com.asdru.asdrulet5.party.exception.AlreadyLootedException.class);
    }

    @Test
    void lootChestOutOfTurnOrderThrows() {
        PartyStateDto created = partyService.createParty(leader);
        partyService.joinParty(created.code(), member);
        partyService.selectClass(created.code(), "leader-1", CharacterClass.BERSERKER);
        partyService.selectClass(created.code(), "player-2", CharacterClass.HEALER);
        partyService.startGame(created.code(), "leader-1", List.of("leader-1", "player-2"));
        when(dungeonService.enteredRoomType(created.code())).thenReturn(RoomType.LOOT);

        assertThatThrownBy(() -> partyService.lootChest(created.code(), "player-2"))
                .isInstanceOf(com.asdru.asdrulet5.party.exception.NotYourLootTurnException.class);
    }

    @Test
    void lootChestInTurnOrderSucceedsForEachMemberInSequence() {
        PartyStateDto created = partyService.createParty(leader);
        partyService.joinParty(created.code(), member);
        partyService.selectClass(created.code(), "leader-1", CharacterClass.BERSERKER);
        partyService.selectClass(created.code(), "player-2", CharacterClass.HEALER);
        // Deliberately not join order, to prove this is genuinely turnOrder-driven.
        partyService.startGame(created.code(), "leader-1", List.of("player-2", "leader-1"));
        when(dungeonService.enteredRoomType(created.code())).thenReturn(RoomType.LOOT);

        assertThatThrownBy(() -> partyService.lootChest(created.code(), "leader-1"))
                .isInstanceOf(com.asdru.asdrulet5.party.exception.NotYourLootTurnException.class);

        assertThatCode(() -> partyService.lootChest(created.code(), "player-2")).doesNotThrowAnyException();
        assertThatCode(() -> partyService.lootChest(created.code(), "leader-1")).doesNotThrowAnyException();
    }

    @Test
    void lootChestNeverClearsTheRoomByItself() {
        // Clearing is driven entirely by acknowledgeLootResult now (see
        // below) — looting alone, even by every member, must never clear
        // the room on its own.
        PartyStateDto created = partyService.createParty(leader);
        partyService.joinParty(created.code(), member);
        partyService.selectClass(created.code(), "leader-1", CharacterClass.BERSERKER);
        partyService.selectClass(created.code(), "player-2", CharacterClass.HEALER);
        partyService.startGame(created.code(), "leader-1", List.of("leader-1", "player-2"));
        when(dungeonService.enteredRoomType(created.code())).thenReturn(RoomType.LOOT);

        partyService.lootChest(created.code(), "leader-1");
        partyService.lootChest(created.code(), "player-2");

        verify(dungeonService, never()).clearEnteredNode(created.code());
    }

    @Test
    void acknowledgeLootBeforeLootingThrows() {
        PartyStateDto created = partyService.createParty(leader);

        assertThatThrownBy(() -> partyService.acknowledgeLootResult(created.code(), "leader-1"))
                .isInstanceOf(com.asdru.asdrulet5.party.exception.NotYetLootedException.class);
    }

    @Test
    void acknowledgeLootByUnknownMemberThrows() {
        PartyStateDto created = partyService.createParty(leader);

        assertThatThrownBy(() -> partyService.acknowledgeLootResult(created.code(), "nobody"))
                .isInstanceOf(NotPartyMemberException.class);
    }

    @Test
    void acknowledgeLootClearsTheRoomOnlyOnceEveryMemberHasAcknowledged() {
        PartyStateDto created = partyService.createParty(leader);
        partyService.joinParty(created.code(), member);
        partyService.selectClass(created.code(), "leader-1", CharacterClass.BERSERKER);
        partyService.selectClass(created.code(), "player-2", CharacterClass.HEALER);
        partyService.startGame(created.code(), "leader-1", List.of("leader-1", "player-2"));
        when(dungeonService.enteredRoomType(created.code())).thenReturn(RoomType.LOOT);
        partyService.lootChest(created.code(), "leader-1");
        partyService.lootChest(created.code(), "player-2");

        partyService.acknowledgeLootResult(created.code(), "leader-1");
        verify(dungeonService, never()).clearEnteredNode(created.code());

        partyService.acknowledgeLootResult(created.code(), "player-2");
        verify(dungeonService, times(1)).clearEnteredNode(created.code());
    }

    @Test
    void acknowledgeLootSoloClearsImmediately() {
        PartyStateDto created = partyService.createParty(leader);
        partyService.selectClass(created.code(), "leader-1", CharacterClass.BERSERKER);
        partyService.startGame(created.code(), "leader-1", List.of("leader-1"));
        when(dungeonService.enteredRoomType(created.code())).thenReturn(RoomType.LOOT);
        partyService.lootChest(created.code(), "leader-1");

        partyService.acknowledgeLootResult(created.code(), "leader-1");

        verify(dungeonService, times(1)).clearEnteredNode(created.code());
    }

    /**
     * The actual roll is randomized (see PartyService's SecureRandom field),
     * so this drives enough independent chest-openings to observe all three
     * find shapes (coins-only, item-only, both) and checks each roll's own
     * mutation matches what it claims to have found, rather than asserting
     * on one specific outcome.
     */
    @Test
    void lootChestAlwaysYieldsCoinsAnItemOrBoth() {
        boolean sawCoinsOnly = false;
        boolean sawItemOnly = false;
        boolean sawBoth = false;
        for (int i = 0; i < 150 && !(sawCoinsOnly && sawItemOnly && sawBoth); i++) {
            PartyStateDto created = partyService.createParty(
                    new AuthenticatedUser("loot-" + i, "Looter", "avatar.png"));
            when(dungeonService.enteredRoomType(created.code())).thenReturn(RoomType.LOOT);
            String userId = "loot-" + i;
            partyService.selectClass(created.code(), userId, CharacterClass.BERSERKER);
            partyService.startGame(created.code(), userId, List.of(userId));

            PartyStateDto updated = partyService.lootChest(created.code(), userId);

            LootResultDto result = updated.lootResults().get(userId);
            assertThat(result.coins() > 0 || !result.itemIds().isEmpty()).isTrue();
            if (result.coins() > 0) {
                assertThat(updated.coins()).isEqualTo(result.coins());
            }
            if (!result.itemIds().isEmpty()) {
                PartyMemberDto self = updated.members().getFirst();
                boolean hasNewGear = self.loadout().weaponItemId() != null
                        || self.loadout().chestplateItemId() != null
                        || self.loadout().trinketItemId() != null;
                assertThat(hasNewGear).isTrue();
            }
            if (result.coins() > 0 && result.itemIds().isEmpty()) sawCoinsOnly = true;
            if (result.coins() == 0 && !result.itemIds().isEmpty()) sawItemOnly = true;
            if (result.coins() > 0 && !result.itemIds().isEmpty()) sawBoth = true;
        }
        assertThat(sawCoinsOnly).as("saw a coins-only find").isTrue();
        assertThat(sawItemOnly).as("saw an item-only find").isTrue();
        assertThat(sawBoth).as("saw a coins-and-item find").isTrue();
    }
}
