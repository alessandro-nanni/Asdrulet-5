package com.asdru.asdrulet5.party;

import com.asdru.asdrulet5.auth.AuthenticatedUser;
import com.asdru.asdrulet5.combat.CombatService;
import com.asdru.asdrulet5.combat.CombatVictoryEvent;
import com.asdru.asdrulet5.dungeon.DungeonService;
import com.asdru.asdrulet5.dungeon.domain.RoomType;
import com.asdru.asdrulet5.inventory.ItemDefinitionRegistry;
import com.asdru.asdrulet5.inventory.domain.ItemDefinition;
import com.asdru.asdrulet5.party.dev.FakeNameGenerator;
import com.asdru.asdrulet5.party.domain.CharacterClass;
import com.asdru.asdrulet5.party.domain.Party;
import com.asdru.asdrulet5.party.domain.PartyMember;
import com.asdru.asdrulet5.party.exception.NotAFakeMemberException;
import com.asdru.asdrulet5.party.exception.NotPartyMemberException;
import com.asdru.asdrulet5.party.exception.PartyNotFoundException;
import com.asdru.asdrulet5.party.web.PartyMapper;
import com.asdru.asdrulet5.party.web.dto.PartyStateDto;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class PartyService {

    /**
     * How long a won fight stays on screen before the party is pulled back
     * to the dungeon map. Without this gap, the party-status broadcast
     * (DUNGEON) can reach the frontend so close behind the combat action's
     * own HTTP response (PARTY_WON) that the dungeon screen swaps back in
     * before the player ever sees the victory banner.
     */
    private static final long VICTORY_DISPLAY_DELAY_SECONDS = 3;

    private final PartyRepository partyRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final DungeonService dungeonService;
    private final CombatService combatService;
    private final ItemDefinitionRegistry itemDefinitionRegistry;
    private final ScheduledExecutorService victoryReturnScheduler;
    private final RoomEntryDelay roomEntryDelay;

    public PartyStateDto createParty(AuthenticatedUser leader) {
        String code = partyRepository.generateUniqueCode();
        Party party = new Party(code, leader.id(), leader.displayName(), leader.avatarUrl());
        partyRepository.save(party);
        return PartyMapper.toDto(party);
    }

    public PartyStateDto joinParty(String code, AuthenticatedUser user) {
        Party party = getOrThrow(code);
        party.addMember(user.id(), user.displayName(), user.avatarUrl());
        return broadcast(party);
    }

    public PartyStateDto startGame(String code, String requesterId, List<String> order) {
        Party party = getOrThrow(code);
        party.start(requesterId, order);
        PartyStateDto dto = broadcast(party);
        dungeonService.startDungeon(party.code(), party.leaderId());
        return dto;
    }

    /**
     * Commits to whatever next room is currently selected on the dungeon
     * map. Fight/boss rooms drop the party into combat (status flips to
     * IN_PROGRESS, mirroring the old enterCombat flow); anything else has no
     * gameplay of its own yet, so it's cleared immediately, unlocking the
     * next set of choices right away.
     *
     * <p>roomEntryDelay.sleep() holds the transition (and therefore the
     * broadcast announcing it) back briefly so the frontend's room-entry
     * swirl animation has time to actually play before the screen changes —
     * see {@link RoomEntryDelay}'s own comment for why this can't just be a
     * client-side delay.
     */
    public PartyStateDto enterRoom(String code, String requesterId) {
        Party party = getOrThrow(code);
        RoomType enteredRoomType = dungeonService.enterNode(code, requesterId);
        roomEntryDelay.sleep();
        if (enteredRoomType == RoomType.FIGHT || enteredRoomType == RoomType.BOSS) {
            party.enterCombat(requesterId);
            PartyStateDto dto = broadcast(party);
            combatService.startCombat(party.code(), party.members(), party.turnOrder());
            return dto;
        }
        dungeonService.clearEnteredNode(code);
        return PartyMapper.toDto(party);
    }

    /**
     * Fired by CombatService the instant a Combat's status flips to
     * PARTY_WON. The actual return-to-dungeon transition is scheduled a few
     * seconds out rather than applied immediately, so the victory banner
     * has time to actually be seen — see VICTORY_DISPLAY_DELAY_SECONDS.
     */
    @EventListener
    public void onCombatVictory(CombatVictoryEvent event) {
        victoryReturnScheduler.schedule(
                () -> returnToDungeonAfterVictory(event.partyCode()),
                VICTORY_DISPLAY_DELAY_SECONDS,
                TimeUnit.SECONDS);
    }

    private void returnToDungeonAfterVictory(String code) {
        Party party = getOrThrow(code);
        dungeonService.clearEnteredNode(code);
        party.returnToDungeon();
        broadcast(party);
    }

    public PartyStateDto equipItem(String code, String userId, String itemId) {
        Party party = getOrThrow(code);
        ItemDefinition definition = itemDefinitionRegistry.get(itemId);
        party.equipItem(userId, definition.slot(), itemId);
        return broadcast(party);
    }

    public PartyStateDto getState(String code) {
        return PartyMapper.toDto(getOrThrow(code));
    }

    public PartyStateDto addFakeMembers(String code, int count) {
        Party party = getOrThrow(code);
        for (int i = 0; i < count; i++) {
            party.addFakeMember(FakeNameGenerator.next());
        }
        return broadcast(party);
    }

    public PartyStateDto selectClassAsFakeMember(String code, String fakeMemberId, CharacterClass characterClass) {
        Party party = getOrThrow(code);
        PartyMember target = party.members().stream()
                .filter(member -> member.userId().equals(fakeMemberId))
                .findFirst()
                .orElseThrow(() -> new NotPartyMemberException(code, fakeMemberId));
        if (!target.bot()) {
            throw new NotAFakeMemberException(code, fakeMemberId);
        }
        party.selectClass(fakeMemberId, characterClass);
        return broadcast(party);
    }

    /**
     * Like {@link #selectClassAsFakeMember}, but for any real (non-bot)
     * member — no bot-ownership guard, since a real member can only ever act
     * as themselves (the id they present is their own).
     */
    public PartyStateDto selectClass(String code, String memberId, CharacterClass characterClass) {
        Party party = getOrThrow(code);
        party.selectClass(memberId, characterClass);
        return broadcast(party);
    }

    private Party getOrThrow(String code) {
        return partyRepository.findByCode(code).orElseThrow(() -> new PartyNotFoundException(code));
    }

    private PartyStateDto broadcast(Party party) {
        PartyStateDto dto = PartyMapper.toDto(party);
        messagingTemplate.convertAndSend("/topic/party/" + party.code(), dto);
        return dto;
    }
}
