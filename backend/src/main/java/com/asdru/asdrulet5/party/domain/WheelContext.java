package com.asdru.asdrulet5.party.domain;

/**
 * What a {@link WheelEffect} needs from the outside world to apply itself —
 * kept as a narrow interface (rather than handing the enum a registry
 * directly) so this domain package doesn't have to depend on
 * ItemDefinitionRegistry/ClassDefinitionRegistry; PartyService supplies an
 * implementation backed by those at the call site.
 */
public interface WheelContext {

    Party party();

    /**
     * The spinning member's own effective max health (base class stats + equipped item bonuses).
     */
    int effectiveMaxHealth(PartyMember member);

    /**
     * Equips a random item (favoring one not already in play anywhere in the
     * party) directly onto member's own gear — see Party.giveAndEquipItem
     * for why this is a personal reward, not a shared-storage drop.
     */
    void giveRandomItemTo(PartyMember member);
}
