package com.asdru.asdrulet5.classdata.domain;

/**
 * What an ability actually does, applied directly to whichever combatant it
 * resolved against (friendly or hostile — that distinction is decided by the
 * ability's {@link TargetType} and resolved before apply() is called, so the
 * effect itself doesn't need to branch on it). For area effects, apply() is
 * invoked once per resolved (alive) target.
 *
 * <p>Open functional interface rather than a closed set of records: define a
 * new effect by writing whatever {@code (actor, target) -> ...} logic you
 * want against the {@link EffectTarget} primitives — damage, heal, buffs,
 * ultimate charge, lifesteal, execute-on-low-health, whatever the design
 * calls for. The static factories below cover the common cases.
 */
@FunctionalInterface
public interface AbilityEffect {

    /**
     * Defense value at which incoming damage is mitigated by exactly 50%.
     * Mitigation fraction is {@code defense / (defense + DEFENSE_HALF_POINT)}
     * — proportional and diminishing-returns rather than a flat subtraction,
     * so defense trims a percentage of a hit instead of a fixed amount. That
     * matters most for small hits (e.g. multi-hit abilities): flat
     * subtraction floors them to the 1-damage minimum against any
     * respectable defense, while proportional mitigation still lets a
     * meaningful fraction through. It also means stacking defense buffs
     * approaches but never reaches full immunity.
     */
    int DEFENSE_HALF_POINT = 25;

    void apply(EffectTarget actor, EffectTarget target);

    static AbilityEffect damage(int power) {
        if (power <= 0) {
            throw new IllegalArgumentException("power must be positive");
        }
        return (actor, target) -> {
            int amount = mitigatedDamage(power + actor.bonusDamage(), target.effectiveDefense());
            target.applyDamage(amount);
            // Ultimates charge from damage dealt, including a damage-dealing
            // ultimate's own hit — it counts toward building the next one.
            actor.addUltimateCharge(amount);
        };
    }

    private static int mitigatedDamage(int rawPower, int defense) {
        double mitigation = (double) defense / (defense + DEFENSE_HALF_POINT);
        return Math.max(1, (int) Math.round(rawPower * (1 - mitigation)));
    }

    /**
     * Strikes the target {@code hits} times in rapid succession, each hit an
     * independent application of {@link #damage(int)} — its own defense
     * reduction, its own 1-damage floor, its own ultimate-charge contribution.
     */
    static AbilityEffect multiHitDamage(int hits, int powerPerHit) {
        if (hits <= 0) {
            throw new IllegalArgumentException("hits must be positive");
        }
        AbilityEffect singleHit = damage(powerPerHit);
        return (actor, target) -> {
            for (int i = 0; i < hits; i++) {
                singleHit.apply(actor, target);
            }
        };
    }

    static AbilityEffect heal(int power) {
        if (power <= 0) {
            throw new IllegalArgumentException("power must be positive");
        }
        return (actor, target) -> target.applyHeal(power);
    }

    /**
     * {@code name} identifies this buff for reapplication purposes (see
     * {@link ActiveEffect}) — typically the ability's own name, so casting
     * the same ability again refreshes its buff instead of stacking a
     * duplicate, while different abilities stay independent. {@code icon} is
     * the display icon key (see {@link ActiveEffect}).
     */
    static AbilityEffect buffDefense(String name, String icon, int power, int durationTurns) {
        if (power <= 0) {
            throw new IllegalArgumentException("power must be positive");
        }
        if (durationTurns <= 0) {
            throw new IllegalArgumentException("durationTurns must be positive");
        }
        return (actor, target) -> target.addActiveEffect(ActiveEffect.defenseBuff(name, icon, power, durationTurns));
    }

    static AbilityEffect buffDamage(String name, String icon, int power, int durationTurns) {
        if (power <= 0) {
            throw new IllegalArgumentException("power must be positive");
        }
        if (durationTurns <= 0) {
            throw new IllegalArgumentException("durationTurns must be positive");
        }
        return (actor, target) -> target.addActiveEffect(ActiveEffect.damageBuff(name, icon, power, durationTurns));
    }

    static AbilityEffect damageOverTime(String name, String description, String icon, int powerPerTurn, int durationTurns) {
        return (actor, target) -> target.addActiveEffect(ActiveEffect.damageOverTime(name, description, icon, powerPerTurn, durationTurns));
    }

    static AbilityEffect healOverTime(String name, String description, String icon, int powerPerTurn, int durationTurns) {
        return (actor, target) -> target.addActiveEffect(ActiveEffect.healOverTime(name, description, icon, powerPerTurn, durationTurns));
    }
}
