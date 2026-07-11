package com.asdru.asdrulet5.party;

import com.asdru.asdrulet5.auth.AuthenticatedUser;
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
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PartyService {

    private final PartyRepository partyRepository;
    private final SimpMessagingTemplate messagingTemplate;

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

    public PartyStateDto setTurnOrder(String code, AuthenticatedUser user, List<String> order) {
        Party party = getOrThrow(code);
        party.setTurnOrder(user.id(), order);
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

    private Party getOrThrow(String code) {
        return partyRepository.findByCode(code).orElseThrow(() -> new PartyNotFoundException(code));
    }

    private PartyStateDto broadcast(Party party) {
        PartyStateDto dto = PartyMapper.toDto(party);
        messagingTemplate.convertAndSend("/topic/party/" + party.code(), dto);
        return dto;
    }
}
