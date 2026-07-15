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

    /**
     * Same shape as {@link #damagePercent()}, but re-evaluated fresh on
     * every single hit instead of being summed once when the wearer's
     * Combatant is built — for bonuses that depend on live combat state
     * (Scythe, scaling off {@code wearer.deadAllyCount()}) or that are
     * themselves randomized per swing (Lucky Charm's crit chance). 0 by
     * default.
     */
    default int damagePercentBonus(EffectTarget wearer) {
        return 0;
    }

    /**
     * Whether this wearer's basic ability should immediately resolve a
     * second time against the same targets, free of an extra stamina cost —
     * rolled once per {@code useAbility} call, only for basic abilities
     * (never ultimates). See Twitching Talisman. False by default.
     */
    default boolean triggersFollowUpAbility() {
        return false;
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

    /** Same condition as {@link #damagePercentIfHealthierThanLeader()}, but a percentage of base max health instead. */
    default int bonusMaxHealthPercentIfHealthierThanLeader() {
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
