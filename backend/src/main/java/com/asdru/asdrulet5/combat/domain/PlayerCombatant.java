package com.asdru.asdrulet5.combat.domain;

import com.asdru.asdrulet5.classdata.domain.Ability;
import com.asdru.asdrulet5.classdata.domain.Stats;
import com.asdru.asdrulet5.inventory.domain.ItemPassive;
import com.asdru.asdrulet5.party.domain.CharacterClass;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * A party member's side of a fight — the only side that has a {@link CharacterClass} and wears items.
 */
@Getter
@Accessors(fluent = true)
public final class PlayerCombatant extends Combatant {

    private final CharacterClass characterClass;
    /**
     * An extra {@code ItemPassive.damagePercentIfHealthierThanLeader()}-shaped
     * bonus, already resolved once at fight start (see CombatService) —
     * unlike the rest of {@link #damagePercentBonus()}, this can't be
     * recomputed dynamically by the base class since it depends on the
     * party's leader, a concept combat.domain deliberately doesn't know
     * about.
     */
    private final int bonusDamagePercent;

    public PlayerCombatant(String combatantId, String displayName, CharacterClass characterClass, Stats stats,
                           int bonusDamagePercent, int ultimateChargeThreshold, List<Ability> abilities,
                           List<ItemPassive> passives) {
        super(combatantId, displayName, stats, ultimateChargeThreshold, abilities, passives);
        this.characterClass = characterClass;
        this.bonusDamagePercent = bonusDamagePercent;
    }

    @Override
    public boolean enemy() {
        return false;
    }

    @Override
    public int damagePercentBonus() {
        return super.damagePercentBonus() + bonusDamagePercent;
    }
}
