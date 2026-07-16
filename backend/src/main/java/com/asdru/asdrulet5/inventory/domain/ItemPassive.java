package com.asdru.asdrulet5.inventory.domain;

import com.asdru.asdrulet5.classdata.domain.CombatantPassive;
import com.asdru.asdrulet5.classdata.domain.EffectTarget;

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

    default int bonusDamagePercent() {
        return 0;
    }

    /**
     * A {@link #bonusMaxHealth()}-shaped bonus, but a percentage of base max
     * health, checked once against wearer — same shape as
     * {@link #damagePercentBonus}, so a condition like Mantle of the
     * Usurper's "healthier than your leader" (see
     * {@link EffectTarget#healthierThanLeader()}) reads identically whether
     * it's boosting damage or max health. Unlike damagePercentBonus though,
     * this is resolved only once, right when the fight starts (see
     * CombatService), not re-evaluated turn to turn — a wearer's max health
     * is fixed for the whole fight once their Combatant is built, so it
     * can't be re-evaluated later even though the condition itself could
     * change. 0 by default.
     */
    default int bonusMaxHealthPercent(EffectTarget wearer) {
        return 0;
    }
}
