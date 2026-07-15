package com.asdru.asdrulet5.classdata.domain;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.function.Supplier;

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

    /**
     * Called once per ability use (not once per resolved target, unlike
     * {@link #apply}) with the actor's own full living ally roster
     * (including the actor) — for abilities that affect the whole team
     * alongside their primary target(s), e.g. a stamina-restoring ultimate
     * that also strikes a single enemy. No-op by default.
     */
    default void applyToTeam(EffectTarget actor, List<EffectTarget> allies) {
    }

    static AbilityEffect damage(int power) {
        requirePositive(power, "power");
        return (actor, target) -> dealDamage(actor, target, power);
    }

    /**
     * Same as {@link #damage(int)}, but with a chance to double the hit's
     * power before mitigation — critChance is a fraction (0.2 = 20%), rolled
     * independently on every application.
     */
    static AbilityEffect critDamage(int power, double critChance) {
        requirePositive(power, "power");
        return (actor, target) -> {
            boolean isCrit = ThreadLocalRandom.current().nextDouble() < critChance;
            dealDamage(actor, target, isCrit ? power * 2 : power);
        };
    }

    private static void dealDamage(EffectTarget actor, EffectTarget target, int power) {
        int scaledPower = Math.max(1, (int) Math.round(power * (1 + actor.damagePercentBonus() / 100.0)));
        int amount = mitigatedDamage(scaledPower + actor.bonusDamage(), target.effectiveDefense());
        target.applyDamage(amount);
        // Ultimates charge from damage dealt, including a damage-dealing
        // ultimate's own hit — it counts toward building the next one.
        actor.addUltimateCharge(amount);
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

    /** Same as {@link #multiHitDamage(int, int)}, but each individual hit independently rolls a crit — see {@link #critDamage}. */
    static AbilityEffect multiHitCritDamage(int hits, int powerPerHit, double critChance) {
        if (hits <= 0) {
            throw new IllegalArgumentException("hits must be positive");
        }
        AbilityEffect singleHit = critDamage(powerPerHit, critChance);
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

    /** Heals the target, and also strips every currently-negative effect off them — see the Healer's ultimate. */
    static AbilityEffect healAndClearNegativeEffects(int power) {
        AbilityEffect healEffect = heal(power);
        return (actor, target) -> {
            healEffect.apply(actor, target);
            target.clearNegativeActiveEffects();
        };
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

    /**
     * Attaches whatever {@link ActiveEffect} the given supplier produces to
     * the target, with no direct damage/heal of its own — see e.g. Mage's
     * Frost Lock (applies Frozen and nothing else).
     */
    static AbilityEffect applyEffect(Supplier<ActiveEffect> effect) {
        return (actor, target) -> target.addActiveEffect(effect.get());
    }

    /**
     * Damages the target, then attaches an {@link ActiveEffect} onto that
     * same target — e.g. a strike that also marks its victim with Golden
     * Touch or Taunt. effectFactory receives the acting combatant, since some
     * effects (Taunt) need to know who cast them.
     */
    static AbilityEffect damageAndApplyEffect(int power, Function<EffectTarget, ActiveEffect> effectFactory) {
        AbilityEffect damageEffect = damage(power);
        return (actor, target) -> {
            damageEffect.apply(actor, target);
            target.addActiveEffect(effectFactory.apply(actor));
        };
    }

    /**
     * Damages a single target, and separately restores staminaAmount to
     * every one of the actor's living allies (including the actor) — see
     * Mage's ultimate. The stamina restore isn't per-resolved-target (there's
     * only ever one damage target here); it runs once via {@link #applyToTeam}.
     */
    static AbilityEffect damageWithTeamStaminaBoost(int power, int staminaAmount) {
        AbilityEffect damageEffect = damage(power);
        return new AbilityEffect() {
            @Override
            public void apply(EffectTarget actor, EffectTarget target) {
                damageEffect.apply(actor, target);
            }

            @Override
            public void applyToTeam(EffectTarget actor, List<EffectTarget> allies) {
                allies.forEach(ally -> ally.restoreStamina(staminaAmount));
            }
        };
    }

    /**
     * Applies Taunt (targeting the actor) to every resolved target, and
     * grants the actor Thorns — see Paladin's ultimate. Called once per
     * enemy for an ALL_ENEMIES ability, so the actor's own Thorns application
     * just refreshes redundantly on each call rather than stacking.
     */
    static AbilityEffect tauntAndSelfThorns(int tauntDurationTurns, int thornsDurationTurns) {
        return (actor, target) -> {
            target.addActiveEffect(ActiveEffect.taunt("Taunt", "taunt", tauntDurationTurns, actor.id()));
            actor.addActiveEffect(ActiveEffect.thorns("Thorns", "thorns", thornsDurationTurns));
        };
    }

    private static void requirePositive(int value, String field) {
        if (value <= 0) {
            throw new IllegalArgumentException(field + " must be positive");
        }
    }
}
