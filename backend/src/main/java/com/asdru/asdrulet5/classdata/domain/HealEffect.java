package com.asdru.asdrulet5.classdata.domain;

public record HealEffect(int power) implements AbilityEffect {
    public HealEffect {
        if (power <= 0) {
            throw new IllegalArgumentException("power must be positive");
        }
    }

    @Override
    public void apply(EffectTarget actor, EffectTarget target) {
        target.applyHeal(power);
    }
}
