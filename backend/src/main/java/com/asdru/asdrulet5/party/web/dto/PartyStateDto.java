package com.asdru.asdrulet5.party.web.dto;

import com.asdru.asdrulet5.party.domain.PartyStatus;

import java.util.List;

public record PartyStateDto(
        String code,
        String leaderId,
        List<PartyMemberDto> members,
        List<String> turnOrder,
        PartyStatus status
) {
}
