package com.asdru.asdrulet5.inventory.domain;

import com.asdru.asdrulet5.classdata.domain.EffectTarget;

/**
 * What an equipped item does. All-default, no-op interface — mirroring how
 * {@link com.asdru.asdrulet5.classdata.domain.ActiveEffect} combines
 * continuous stat getters with an event hook — so an item only needs to
 * override whichever pieces are relevant to it: a flat stat boost overrides
 * one of the {@code bonusX()} methods, a reactive item (lifesteal, thorns,
 * heal-on-kill, ...) overrides one of the {@code onX()} hooks, and nothing
 * stops an item doing both.
 *
 * <p>Operates on {@link EffectTarget} rather than combat.domain.Combatant
 * directly, for the same reason {@code AbilityEffect} does: it lets
 * inventory depend on classdata without depending on combat, while combat
 * (which already depends on inventory for item lookups) supplies its
 * Combatant instances — which implement EffectTarget — as the hook
 * arguments.
 */
public interface ItemPassive {

    default int bonusMaxHealth() {
        return 0;
    }

    default int bonusMaxStamina() {
        return 0;
    }

    default int bonusDefense() {
        return 0;
    }

    /** Percentage modifier applied to an ability's own power (e.g. 20 = +20%, -10 = -10%). */
    default int damagePercent() {
        return 0;
    }

    /** Called on the wearer when it becomes their turn, after active effects tick. */
    default void onStartTurn(EffectTarget wearer) {
    }

    /** Called on the wearer when they end their turn. */
    default void onEndTurn(EffectTarget wearer) {
    }

    /** Called on the wearer after they deal {@code amount} damage to target. */
    default void onDamageDealt(EffectTarget wearer, EffectTarget target, int amount) {
    }

    /** Called on the wearer after they take {@code amount} damage from attacker. */
    default void onDamageTaken(EffectTarget wearer, EffectTarget attacker, int amount) {
    }

    /** Called on the wearer after one of their hits reduces victim to 0 health. */
    default void onKill(EffectTarget wearer, EffectTarget victim) {
    }

    /** Called on the wearer when their own health reaches 0. */
    default void onDeath(EffectTarget wearer) {
    }
}
