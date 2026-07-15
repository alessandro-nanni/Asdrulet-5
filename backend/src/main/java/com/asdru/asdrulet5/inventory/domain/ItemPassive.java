package com.asdru.asdrulet5.inventory.domain;

import com.asdru.asdrulet5.classdata.domain.CombatantPassive;

/**
 * What an equipped item does, beyond what any combatant's passive can do
 * (see {@link CombatantPassive}) — the gear-specific stat bonuses that only
 * make sense for a party member wearing equipment: flat max-stat boosts,
 * and the party-leader-relative bonuses.
 */
public interface ItemPassive extends CombatantPassive {

    default int bonusMaxHealth() {
        return 0;
    }

    default int bonusMaxStamina() {
        return 0;
    }

    default int bonusDefense() {
        return 0;
    }

    /**
     * Extra {@link #damagePercent()}-shaped bonus granted only while this
     * wearer has more current health than their party's leader — checked
     * once, when the fight starts (see CombatService), not re-evaluated
     * turn to turn. See Mantle of the Usurper. 0 by default.
     */
    default int damagePercentIfHealthierThanLeader() {
        return 0;
    }

    /**
     * Same condition as {@link #damagePercentIfHealthierThanLeader()}, but a percentage of base max health instead.
     */
    default int bonusMaxHealthPercentIfHealthierThanLeader() {
        return 0;
    }
}
