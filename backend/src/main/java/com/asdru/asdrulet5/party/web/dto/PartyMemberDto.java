package com.asdru.asdrulet5.party.web.dto;

import com.asdru.asdrulet5.party.domain.CharacterClass;

import java.util.List;

public record PartyMemberDto(
        String userId,
        String displayName,
        String avatarUrl,
        CharacterClass characterClass,
        boolean leader,
        boolean bot,
        LoadoutDto loadout,
        Integer currentHealth,
        List<PendingEffectDto> pendingEffects
) {
}
