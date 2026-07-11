package com.asdru.asdrulet5.party.domain;

public record PartyMember(
        String userId,
        String displayName,
        String avatarUrl,
        CharacterClass characterClass,
        boolean leader
) {

    public PartyMember withCharacterClass(CharacterClass newClass) {
        return new PartyMember(userId, displayName, avatarUrl, newClass, leader);
    }
}
