package com.asdru.asdrulet5.inventory.domain;

import java.util.Random;

/**
 * How many distinct entries a single {@link LootTable} roll draws — either
 * fixed (a mystery wheel's exactly 1, a merchant's exactly 8) or a random
 * range (a chest's 1-3). {@code min == max} represents a fixed amount.
 */
public record LootAmount(int min, int max) {

    public LootAmount {
        if (min <= 0) {
            throw new IllegalArgumentException("min must be positive");
        }
        if (max < min) {
            throw new IllegalArgumentException("max must not be less than min");
        }
    }

    public static LootAmount fixed(int amount) {
        return new LootAmount(amount, amount);
    }

    /** Rolls a single amount within [min, max], inclusive. */
    public int roll(Random random) {
        return min == max ? min : min + random.nextInt(max - min + 1);
    }
}
