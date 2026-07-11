package com.asdru.asdrulet5.party.web.dto;

import com.asdru.asdrulet5.party.domain.CharacterClass;

public record PartyMemberDto(
        String userId,
        String displayName,
        String avatarUrl,
        CharacterClass characterClass,
        boolean leader,
        boolean bot
) {
}
