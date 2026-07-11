package com.asdru.asdrulet5.classdata.domain;

/**
 * What stat a temporary {@link BuffDefenseEffect}/{@link BuffDamageEffect}
 * modifies. Narrower than the old EffectType — instant effects (damage/heal)
 * don't need a "kind" at all now that they're their own classes.
 */
public enum BuffKind {
    DEFENSE,
    DAMAGE
}
