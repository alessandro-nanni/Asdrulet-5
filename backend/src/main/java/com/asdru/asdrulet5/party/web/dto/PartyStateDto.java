package com.asdru.asdrulet5.party.web.dto;

import com.asdru.asdrulet5.party.domain.PartyStatus;
import com.asdru.asdrulet5.party.domain.WheelEffect;

import java.util.List;
import java.util.Map;

public record PartyStateDto(
        String code,
        String leaderId,
        List<PartyMemberDto> members,
        List<String> turnOrder,
        PartyStatus status,
        List<String> storage,
        Map<String, WheelEffect> wheelResults
) {
}
