package com.asdru.asdrulet5.inventory.domain;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LootAmountTest {

    @Test
    void fixedAlwaysRollsTheSameAmount() {
        LootAmount amount = LootAmount.fixed(8);
        Random random = new Random(1);

        for (int i = 0; i < 20; i++) {
            assertThat(amount.roll(random)).isEqualTo(8);
        }
    }

    @Test
    void rangeRollsWithinBoundsInclusive() {
        LootAmount amount = new LootAmount(1, 3);
        Random random = new Random(1);

        boolean sawOne = false;
        boolean sawThree = false;
        for (int i = 0; i < 200; i++) {
            int rolled = amount.roll(random);
            assertThat(rolled).isBetween(1, 3);
            sawOne |= rolled == 1;
            sawThree |= rolled == 3;
        }
        assertThat(sawOne).as("saw the minimum").isTrue();
        assertThat(sawThree).as("saw the maximum").isTrue();
    }

    @Test
    void nonPositiveMinThrows() {
        assertThatThrownBy(() -> new LootAmount(0, 3)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void maxBelowMinThrows() {
        assertThatThrownBy(() -> new LootAmount(3, 1)).isInstanceOf(IllegalArgumentException.class);
    }
}
