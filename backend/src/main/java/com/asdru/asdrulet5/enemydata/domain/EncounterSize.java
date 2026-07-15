package com.asdru.asdrulet5.enemydata.domain;

import java.util.Random;

/**
 * How many enemies a single {@link EncounterTable} roll spawns — either
 * fixed (a boss's exactly 1) or a random range (a regular floor-1 fight's
 * 2-3). {@code min == max} represents a fixed amount.
 */
public record EncounterSize(int min, int max) {

    public EncounterSize {
        if (min <= 0) {
            throw new IllegalArgumentException("min must be positive");
        }
        if (max < min) {
            throw new IllegalArgumentException("max must not be less than min");
        }
    }

    public static EncounterSize fixed(int amount) {
        return new EncounterSize(amount, amount);
    }

    /**
     * Rolls a single amount within [min, max], inclusive.
     */
    public int roll(Random random) {
        return min == max ? min : min + random.nextInt(max - min + 1);
    }
}
