package com.asdru.asdrulet5.party.domain;

import com.asdru.asdrulet5.classdata.domain.ActiveEffect;
import com.asdru.asdrulet5.inventory.domain.Loadout;

import java.util.List;

/**
 * currentHealth is null whenever the member is at full health — matching a
 * freshly-created member who's never been touched by anything yet, and
 * mirroring how a "full heal" result just clears it back to null rather than
 * having to know the member's actual max health. pendingEffects reuses the
 * same {@link ActiveEffect} classdata already applies during combat (e.g. a
 * MYSTERY wheel's "poison" result is just a damage-over-time ActiveEffect
 * sitting here until the next fight starts) — CombatService seeds a fresh
 * Combatant's own activeEffects from whatever's here. Both fields only ever
 * change via a wheel spin today; nothing here is touched by combat itself,
 * so a member's health/pending effects carry unchanged from one fight's end
 * into the next room, but a fight's own outcome is never written back onto
 * it.
 */
public record PartyMember(
        String userId,
        String displayName,
        String avatarUrl,
        CharacterClass characterClass,
        boolean leader,
        boolean bot,
        Loadout loadout,
        Integer currentHealth,
        List<ActiveEffect> pendingEffects
) {

    public PartyMember {
        pendingEffects = List.copyOf(pendingEffects);
    }

    public PartyMember withCharacterClass(CharacterClass newClass) {
        return new PartyMember(userId, displayName, avatarUrl, newClass, leader, bot, loadout,
                currentHealth, pendingEffects);
    }

    public PartyMember withLoadout(Loadout newLoadout) {
        return new PartyMember(userId, displayName, avatarUrl, characterClass, leader, bot, newLoadout,
                currentHealth, pendingEffects);
    }

    public PartyMember withCurrentHealth(Integer newCurrentHealth) {
        return new PartyMember(userId, displayName, avatarUrl, characterClass, leader, bot, loadout,
                newCurrentHealth, pendingEffects);
    }

    public PartyMember withPendingEffects(List<ActiveEffect> newPendingEffects) {
        return new PartyMember(userId, displayName, avatarUrl, characterClass, leader, bot, loadout,
                currentHealth, newPendingEffects);
    }
}
