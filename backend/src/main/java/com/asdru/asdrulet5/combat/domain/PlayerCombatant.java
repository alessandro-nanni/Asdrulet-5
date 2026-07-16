package com.asdru.asdrulet5.combat.domain;

import com.asdru.asdrulet5.classdata.domain.Ability;
import com.asdru.asdrulet5.classdata.domain.Stats;
import com.asdru.asdrulet5.inventory.domain.ItemPassive;
import com.asdru.asdrulet5.party.domain.CharacterClass;
import com.asdru.asdrulet5.party.domain.Party;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * A party member's side of a fight — the only side that has a
 * {@link CharacterClass}, wears items, and belongs to a {@link Party}.
 */
@Getter
@Accessors(fluent = true)
public final class PlayerCombatant extends Combatant {

    private final CharacterClass characterClass;
    /**
     * The party this combatant belongs to. Lets a combat-context-dependent
     * item passive (see Mantle of the Usurper / {@link #healthierThanLeader()})
     * reach the leader's own live data directly through this wearer, instead
     * of CombatService having to precompute and thread through a narrow,
     * purpose-built value for every such passive.
     */
    private final Party party;

    public PlayerCombatant(String combatantId, String displayName, CharacterClass characterClass, Stats stats,
                           int ultimateChargeThreshold, List<Ability> abilities, List<ItemPassive> passives,
                           Party party) {
        super(combatantId, displayName, stats, ultimateChargeThreshold, abilities, passives);
        this.characterClass = characterClass;
        this.party = party;
    }

    @Override
    public boolean enemy() {
        return false;
    }

    /**
     * The living PlayerCombatant filling this fight's leader slot, found via
     * the shared roster (attached once the whole fight exists — see
     * {@link Combat}'s constructor) rather than {@link Party#members()},
     * since only the roster reflects live, mid-fight health. Null in the
     * never-expected case the leader isn't actually part of this fight.
     */
    public PlayerCombatant leader() {
        return roster().stream()
                .filter(PlayerCombatant.class::isInstance)
                .map(PlayerCombatant.class::cast)
                .filter(combatant -> combatant.combatantId().equals(party.leaderId()))
                .findFirst()
                .orElse(null);
    }

    @Override
    public boolean healthierThanLeader() {
        PlayerCombatant leader = leader();
        return leader != null && leader != this && currentHealth() > leader.currentHealth();
    }
}
