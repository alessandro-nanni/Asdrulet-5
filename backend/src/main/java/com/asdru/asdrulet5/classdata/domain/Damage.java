package com.asdru.asdrulet5.classdata.domain;

/**
 * A single damage application's payload — {@code amount} is the raw power
 * about to hit {@link EffectTarget#applyDamage}, before that target's own
 * clamping (a lethal hit's recorded {@code CombatEvent} may end up smaller
 * than this if it overkills). {@code critical} is carried alongside it
 * rather than folded into {@code amount} so hooks and events downstream
 * (item passives, active effects, the frontend) can react to *how* the
 * damage landed, not just how much.
 */
public record Damage(int amount, boolean critical) {

    public Damage {
        if (amount < 0) {
            throw new IllegalArgumentException("amount must not be negative");
        }
    }

    /**
     * A non-critical hit for the given amount — the common case.
     */
    public static Damage of(int amount) {
        return new Damage(amount, false);
    }
}
