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

@Service
@RequiredArgsConstructor
public class PartyService {

    private final PartyRepository partyRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final DungeonService dungeonService;
    private final CombatService combatService;
    private final ItemDefinitionRegistry itemDefinitionRegistry;

    public PartyStateDto createParty(AuthenticatedUser leader, String displayName) {
        String code = partyRepository.generateUniqueCode();
        Party party = new Party(code, leader.id(), displayName, leader.avatarUrl());
        partyRepository.save(party);
        return PartyMapper.toDto(party);
    }

    public PartyStateDto joinParty(String code, AuthenticatedUser user, String displayName) {
        Party party = getOrThrow(code);
        party.addMember(user.id(), displayName, user.avatarUrl());
        return broadcast(party);
    }

    public PartyStateDto selectClass(String code, AuthenticatedUser user, CharacterClass characterClass) {
        Party party = getOrThrow(code);
        party.selectClass(user.id(), characterClass);
        return broadcast(party);
    }

    public PartyStateDto startGame(String code, AuthenticatedUser user, List<String> order) {
        return startGame(code, user.id(), order);
    }

    public PartyStateDto startGame(String code, String requesterId, List<String> order) {
        Party party = getOrThrow(code);
        party.start(requesterId, order);
        PartyStateDto dto = broadcast(party);
        dungeonService.startDungeon(party.code(), party.leaderId());
        return dto;
    }

    public PartyStateDto enterRoom(String code, AuthenticatedUser user) {
        return enterRoom(code, user.id());
    }

    /**
     * Commits to whatever next room is currently selected on the dungeon
     * map. Fight/boss rooms drop the party into combat (status flips to
     * IN_PROGRESS, mirroring the old enterCombat flow); anything else has no
     * gameplay of its own yet, so it's cleared immediately, unlocking the
     * next set of choices right away.
     */
    public PartyStateDto enterRoom(String code, String requesterId) {
        Party party = getOrThrow(code);
        RoomType enteredRoomType = dungeonService.enterNode(code, requesterId);
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
     * PARTY_WON — flips the party back out of combat and resolves the
     * dungeon node the fight was fought in, so the party can keep moving.
     */
    @EventListener
    public void onCombatVictory(CombatVictoryEvent event) {
        Party party = getOrThrow(event.partyCode());
        dungeonService.clearEnteredNode(event.partyCode());
        party.returnToDungeon();
        broadcast(party);
    }

    public PartyStateDto equipItem(String code, AuthenticatedUser user, String itemId) {
        return equipItem(code, user.id(), itemId);
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
     * Like {@link #selectClassAsFakeMember}, but for the session-less leader
     * of a dev "quick game" party (see PartyDevSessionController) rather than
     * a bot — that leader is a real (non-bot) member, just without a Google
     * session behind it, so the bot-only guard above doesn't apply here.
     */
    public PartyStateDto selectClassAsMember(String code, String memberId, CharacterClass characterClass) {
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
