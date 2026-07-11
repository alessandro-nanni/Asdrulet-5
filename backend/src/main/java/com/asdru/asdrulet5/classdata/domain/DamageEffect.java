package com.asdru.asdrulet5.classdata.domain;

public record DamageEffect(int power) implements AbilityEffect {
    public DamageEffect {
        if (power <= 0) {
            throw new IllegalArgumentException("power must be positive");
        }
    }

    @Override
    public void apply(EffectTarget actor, EffectTarget target) {
        int amount = Math.max(1, power + actor.bonusDamage() - target.effectiveDefense());
        target.applyDamage(amount);
        // Ultimates charge from damage dealt, including a damage-dealing
        // ultimate's own hit — it counts toward building the next one.
        actor.addUltimateCharge(amount);
    }
}
