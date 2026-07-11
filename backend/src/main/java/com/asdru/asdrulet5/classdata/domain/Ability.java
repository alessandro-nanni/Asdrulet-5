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

    TargetType targetType();
}
