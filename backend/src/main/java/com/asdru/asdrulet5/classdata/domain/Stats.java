package com.asdru.asdrulet5.classdata.domain;

public record Stats(
        int maxHealth,
        int defense,
        int maxStamina
) {
    public Stats {
        if (maxHealth <= 0) {
            throw new IllegalArgumentException("maxHealth must be positive");
        }
        if (defense < 0) {
            throw new IllegalArgumentException("defense must not be negative");
        }
        if (maxStamina < 0) {
            throw new IllegalArgumentException("maxStamina must not be negative");
        }
    }
}
