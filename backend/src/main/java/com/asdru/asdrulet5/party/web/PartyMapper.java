package com.asdru.asdrulet5.party.web;

import com.asdru.asdrulet5.party.domain.Party;
import com.asdru.asdrulet5.party.domain.PartyMember;
import com.asdru.asdrulet5.party.web.dto.PartyMemberDto;
import com.asdru.asdrulet5.party.web.dto.PartyStateDto;

public final class PartyMapper {

    private PartyMapper() {
    }

    public static PartyStateDto toDto(Party party) {
        return new PartyStateDto(
                party.code(),
                party.leaderId(),
                party.members().stream().map(PartyMapper::toDto).toList(),
                party.turnOrder()
        );
    }

    private static PartyMemberDto toDto(PartyMember member) {
        return new PartyMemberDto(
                member.userId(),
                member.displayName(),
                member.avatarUrl(),
                member.characterClass(),
                member.leader()
        );
    }
}
