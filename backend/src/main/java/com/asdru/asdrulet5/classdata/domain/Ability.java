package com.asdru.asdrulet5.classdata.domain;

/**
 * A class ability is either a {@link BasicAbility}, repeatable each turn as
 * long as stamina allows, or the class's single {@link UltimateAbility},
 * gated by a damage-charge meter instead of stamina.
 */
public sealed interface Ability permits BasicAbility, UltimateAbility {
    String id();

    String name();

    String description();

    /**
     * Short mechanical summary for display (e.g. "22 damage", "Heals for
     * 15% of missing HP") — author-written rather than derived, since
     * {@link #effect()} can now be arbitrary logic that doesn't reduce to a
     * single power/duration pair.
     */
    String effectSummary();

    TargetType targetType();

    AbilityEffect effect();
}
