package com.asdru.asdrulet5.party.domain;

import com.asdru.asdrulet5.inventory.domain.Loadout;

public record PartyMember(
        String userId,
        String displayName,
        String avatarUrl,
        CharacterClass characterClass,
        boolean leader,
        boolean bot,
        Loadout loadout
) {

    public PartyMember withCharacterClass(CharacterClass newClass) {
        return new PartyMember(userId, displayName, avatarUrl, newClass, leader, bot, loadout);
    }

    public PartyMember withLoadout(Loadout newLoadout) {
        return new PartyMember(userId, displayName, avatarUrl, characterClass, leader, bot, newLoadout);
    }
}
