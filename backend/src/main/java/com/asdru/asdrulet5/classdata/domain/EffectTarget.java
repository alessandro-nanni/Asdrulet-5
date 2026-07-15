package com.asdru.asdrulet5.classdata.domain;

/**
 * What an {@link AbilityEffect} needs from whatever it's acting on or being
 * applied to. Implemented by combat.domain.Combatant — kept as an interface
 * here (rather than referencing Combatant directly) so classdata, which
 * combat already depends on, doesn't have to depend back on combat.
 */
public interface EffectTarget {

    String id();

    int currentHealth();

    int maxHealth();

    int effectiveDefense();

    int bonusDamage();

    int damagePercentBonus();

    void applyDamage(int amount);

    void applyHeal(int amount);

    /** Restores stamina, capped at max — same clamped-add shape as {@link #applyHeal}, just for the other resource. */
    void restoreStamina(int amount);

    /** Spends stamina, floored at 0 — the wearer-facing counterpart to {@link #restoreStamina}, for items like Satellite Dish that tax their own wearer. */
    void drainStamina(int amount);

    void addActiveEffect(ActiveEffect effect);

    /** Removes every currently-attached effect for which {@link ActiveEffect#isNegative()} is true. */
    void clearNegativeActiveEffects();

    void addUltimateCharge(int amount);

    /** How many of this combatant's own allies (same side, self excluded) are no longer alive — see Scythe. */
    int deadAllyCount();
}
