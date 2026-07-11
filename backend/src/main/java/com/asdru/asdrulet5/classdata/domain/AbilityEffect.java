package com.asdru.asdrulet5.classdata.domain;

/**
 * What an ability actually does, applied directly to whichever combatant it
 * resolved against (friendly or hostile — that distinction is decided by the
 * ability's {@link TargetType} and resolved before apply() is called, so the
 * effect itself doesn't need to branch on it).
 */
public sealed interface AbilityEffect permits DamageEffect, HealEffect, BuffDefenseEffect, BuffDamageEffect {
    void apply(EffectTarget actor, EffectTarget target);
}
