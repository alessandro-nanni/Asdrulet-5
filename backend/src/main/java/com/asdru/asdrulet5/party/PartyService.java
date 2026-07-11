package com.asdru.asdrulet5.party;

import com.asdru.asdrulet5.auth.AuthenticatedUser;
import com.asdru.asdrulet5.party.domain.CharacterClass;
import com.asdru.asdrulet5.party.domain.Party;
import com.asdru.asdrulet5.party.exception.PartyNotFoundException;
import com.asdru.asdrulet5.party.web.PartyMapper;
import com.asdru.asdrulet5.party.web.dto.PartyStateDto;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PartyService {

    private final PartyRepository partyRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public PartyService(PartyRepository partyRepository, SimpMessagingTemplate messagingTemplate) {
        this.partyRepository = partyRepository;
        this.messagingTemplate = messagingTemplate;
    }

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

    private Party getOrThrow(String code) {
        return partyRepository.findByCode(code).orElseThrow(() -> new PartyNotFoundException(code));
    }

    private PartyStateDto broadcast(Party party) {
        PartyStateDto dto = PartyMapper.toDto(party);
        messagingTemplate.convertAndSend("/topic/party/" + party.code(), dto);
        return dto;
    }
}
