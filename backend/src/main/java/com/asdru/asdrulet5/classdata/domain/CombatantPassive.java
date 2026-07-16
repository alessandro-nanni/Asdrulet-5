package com.asdru.asdrulet5.classdata.domain;

/**
 * A passive behavior any combatant can carry — a player's equipped item
 * ({@code com.asdru.asdrulet5.inventory.domain.ItemPassive}) or an enemy's
 * own innate trait ({@code com.asdru.asdrulet5.enemydata.domain.EnemyPassive}).
 * All-default, no-op interface: a passive only needs to override whichever
 * pieces are relevant to it — a flat/scaling damage contributor overrides
 * {@link #damagePercent()} or {@link #damagePercentBonus(EffectTarget)}, a
 * reactive passive (lifesteal, thorns, heal-on-kill, ...) overrides one of
 * the {@code onX()} hooks, and nothing stops a passive doing both.
 *
 * <p>Operates on {@link EffectTarget} rather than combat.domain.Combatant
 * directly, so this package doesn't have to depend on combat — combat
 * (which already depends on classdata) supplies its Combatant instances,
 * which implement EffectTarget, as the hook arguments.
 */
public interface CombatantPassive {

    /**
     * Called on the wearer when it becomes their turn, after active effects tick.
     */
    default void onStartTurn(EffectTarget wearer) {
    }

    /**
     * Called on the wearer when they end their turn.
     */
    default void onEndTurn(EffectTarget wearer) {
    }

    /**
     * Called on the wearer after they deal {@code damage} to target.
     */
    default void onDamageDealt(EffectTarget wearer, EffectTarget target, Damage damage) {
    }

    /**
     * Called on the wearer after they take {@code damage} from attacker.
     */
    default void onDamageTaken(EffectTarget wearer, EffectTarget attacker, Damage damage) {
    }

    /**
     * Called on the wearer after one of their hits reduces victim to 0 health.
     */
    default void onKill(EffectTarget wearer, EffectTarget victim) {
    }

    /**
     * Called on the wearer when their own health reaches 0.
     */
    default void onDeath(EffectTarget wearer) {
    }

    /**
     * Flat percentage modifier applied to the wearer's outgoing damage (e.g. 20 = +20%, -10 = -10%).
     */
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
     * rolled once per ability use, only for basic abilities (never
     * ultimates). See Twitching Talisman. False by default.
     */
    default boolean triggersFollowUpAbility() {
        return false;
    }

    /**
     * Flat percentage modifier applied to healing the wearer receives (e.g.
     * 20.0 = +20% healing received, -50.0 = -50%). A {@code double} rather
     * than {@link #damagePercent()}'s {@code int}, since a healing-received
     * modifier is more likely to need fractional precision (e.g. +12.5%).
     * Takes wearer, same shape as {@link #damagePercentBonus(EffectTarget)},
     * for passives whose bonus depends on live combat state. 0 by default.
     */
    default double healingReceivedPercent(EffectTarget wearer) {
        return 0.0;
    }

    /**
     * Flat bonus stamina the wearer regenerates at the start of their own
     * turn, on top of {@code Combat.STAMINA_REGEN_PER_TURN}. Takes wearer,
     * same shape as {@link #damagePercentBonus(EffectTarget)}, for passives
     * whose bonus depends on live combat state. 0 by default.
     */
    default int staminaRegenBonus(EffectTarget wearer) {
        return 0;
    }
}
