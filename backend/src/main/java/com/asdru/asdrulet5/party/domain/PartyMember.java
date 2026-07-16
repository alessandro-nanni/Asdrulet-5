package com.asdru.asdrulet5.party.domain;

import com.asdru.asdrulet5.classdata.domain.ActiveEffect;
import com.asdru.asdrulet5.inventory.domain.Loadout;

import java.util.List;
import java.util.Set;

/**
 * currentHealth is null whenever the member is at full health — matching a
 * freshly-created member who's never been touched by anything yet, and
 * mirroring how a "full heal" result just clears it back to null rather than
 * having to know the member's actual max health. pendingEffects reuses the
 * same {@link ActiveEffect} classdata already applies during combat (e.g. a
 * MYSTERY wheel's "poison" result is just a damage-over-time ActiveEffect
 * sitting here until the next fight starts) — CombatService seeds a fresh
 * Combatant's own activeEffects from whatever's here. Set by a wheel/loot
 * roll, and also written back here the instant a fight ends (see
 * PartyService.syncMembersAfterCombat) with that fight's own ending health
 * and still-active effects — so both fields always reflect whatever's
 * carrying into the next room, whether that came from a room's own reward
 * or from how the last battle was left. mana is this member's own currency
 * (unlike the party's shared coins) spent on unlockedSkillIds — the ids of
 * every {@code SkillNode} this member has unlocked in their own class's
 * skill tree (see {@code classdata.domain.SkillTreeResolver}, which turns
 * this set into the member's actual effective ability list once they enter
 * combat).
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
        List<ActiveEffect> pendingEffects,
        int mana,
        Set<String> unlockedSkillIds
) {

    public PartyMember {
        pendingEffects = List.copyOf(pendingEffects);
        unlockedSkillIds = Set.copyOf(unlockedSkillIds);
    }

    public PartyMember withCharacterClass(CharacterClass newClass) {
        return new PartyMember(userId, displayName, avatarUrl, newClass, leader, bot, loadout,
                currentHealth, pendingEffects, mana, unlockedSkillIds);
    }

    public PartyMember withLoadout(Loadout newLoadout) {
        return new PartyMember(userId, displayName, avatarUrl, characterClass, leader, bot, newLoadout,
                currentHealth, pendingEffects, mana, unlockedSkillIds);
    }

    public PartyMember withCurrentHealth(Integer newCurrentHealth) {
        return new PartyMember(userId, displayName, avatarUrl, characterClass, leader, bot, loadout,
                newCurrentHealth, pendingEffects, mana, unlockedSkillIds);
    }

    public PartyMember withPendingEffects(List<ActiveEffect> newPendingEffects) {
        return new PartyMember(userId, displayName, avatarUrl, characterClass, leader, bot, loadout,
                currentHealth, newPendingEffects, mana, unlockedSkillIds);
    }

    public PartyMember withMana(int newMana) {
        return new PartyMember(userId, displayName, avatarUrl, characterClass, leader, bot, loadout,
                currentHealth, pendingEffects, newMana, unlockedSkillIds);
    }

    public PartyMember withUnlockedSkillIds(Set<String> newUnlockedSkillIds) {
        return new PartyMember(userId, displayName, avatarUrl, characterClass, leader, bot, loadout,
                currentHealth, pendingEffects, mana, newUnlockedSkillIds);
    }
}
