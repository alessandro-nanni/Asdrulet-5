package com.asdru.asdrulet5.classdata.domain;

public record BuffDefenseEffect(int power, int durationTurns) implements AbilityEffect {
    public BuffDefenseEffect {
        if (power <= 0) {
            throw new IllegalArgumentException("power must be positive");
        }
        if (durationTurns <= 0) {
            throw new IllegalArgumentException("durationTurns must be positive");
        }
    }

    @Override
    public void apply(EffectTarget actor, EffectTarget target) {
        target.addActiveEffect(BuffKind.DEFENSE, power, durationTurns);
    }
}
