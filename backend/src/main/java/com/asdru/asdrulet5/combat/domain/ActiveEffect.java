package com.asdru.asdrulet5.combat.domain;

import com.asdru.asdrulet5.classdata.domain.BuffKind;

import java.util.Objects;

public record ActiveEffect(
        BuffKind kind,
        int power,
        int remainingTurns
) {
    public ActiveEffect {
        Objects.requireNonNull(kind, "kind");
        if (power <= 0) {
            throw new IllegalArgumentException("power must be positive");
        }
        if (remainingTurns <= 0) {
            throw new IllegalArgumentException("remainingTurns must be positive");
        }
    }

    public ActiveEffect withTurnElapsed() {
        return new ActiveEffect(kind, power, remainingTurns - 1);
    }
}
