package com.asdru.asdrulet5.classdata.domain;

/**
 * What an {@link AbilityEffect} needs from whatever it's acting on or being
 * applied to. Implemented by combat.domain.Combatant — kept as an interface
 * here (rather than referencing Combatant directly) so classdata, which
 * combat already depends on, doesn't have to depend back on combat.
 */
public interface EffectTarget {

    /**
     * This fight's own unique identifier for this combatant (e.g. "p1", "enemy-2") — not a species/definition id, see combat.domain.EnemyCombatant's own id() for that.
     */
    String combatantId();

    /**
     * The player- or author-facing name for this combatant (e.g. "Grog", "Goblin Marauder") — for UI-facing text (see ActiveEffect.taunt), never for identity/matching, which is {@link #combatantId()}'s job.
     */
    String displayName();

    int currentHealth();

    int maxHealth();

    int effectiveDefense();

    int bonusDamage();

    int damagePercentBonus();

    void applyDamage(Damage damage);

    void applyHeal(int amount);

    /**
     * Restores stamina, capped at max — same clamped-add shape as {@link #applyHeal}, just for the other resource.
     */
    void restoreStamina(int amount);

    /**
     * Spends stamina, floored at 0 — the wearer-facing counterpart to {@link #restoreStamina}, for items like Satellite Dish that tax their own wearer.
     */
    void drainStamina(int amount);

    void addActiveEffect(ActiveEffect effect);

    /**
     * Removes every currently-attached effect for which {@link ActiveEffect#isNegative()} is true.
     */
    void clearNegativeActiveEffects();

    void addUltimateCharge(int amount);

    /**
     * How many of this combatant's own allies (same side, self excluded) are no longer alive — see Scythe.
     */
    int deadAllyCount();

    /**
     * Whether this combatant currently has more health than their party's
     * leader — see Mantle of the Usurper. False by default (and always, for
     * an enemy): only combat.domain.PlayerCombatant has a notion of "party"
     * to compare itself against.
     */
    default boolean healthierThanLeader() {
        return false;
    }
}
