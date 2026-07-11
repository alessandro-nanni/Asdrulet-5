package com.asdru.asdrulet5.classdata.domain;

public record Stats(
        int maxHealth,
        int damage,
        int defense,
        int speed,
        int maxStamina
) {
    public Stats {
        if (maxHealth <= 0) {
            throw new IllegalArgumentException("maxHealth must be positive");
        }
        if (damage < 0) {
            throw new IllegalArgumentException("damage must not be negative");
        }
        if (defense < 0) {
            throw new IllegalArgumentException("defense must not be negative");
        }
        if (speed <= 0) {
            throw new IllegalArgumentException("speed must be positive");
        }
        if (maxStamina < 0) {
            throw new IllegalArgumentException("maxStamina must not be negative");
        }
    }
}
