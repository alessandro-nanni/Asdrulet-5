package com.asdru.asdrulet5.classdata.domain;

/**
 * What an {@link AbilityEffect} needs from whatever it's acting on or being
 * applied to. Implemented by combat.domain.Combatant — kept as an interface
 * here (rather than referencing Combatant directly) so classdata, which
 * combat already depends on, doesn't have to depend back on combat.
 */
public interface EffectTarget {

    int currentHealth();

    int maxHealth();

    int effectiveDefense();

    int bonusDamage();

    void applyDamage(int amount);

    void applyHeal(int amount);

    void addActiveEffect(ActiveEffect effect);

    void addUltimateCharge(int amount);
}
