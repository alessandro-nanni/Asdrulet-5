package com.asdru.asdrulet5.enemydata.domain;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EncounterSizeTest {

    @Test
    void fixedAlwaysRollsTheSameAmount() {
        EncounterSize size = EncounterSize.fixed(1);
        Random random = new Random(1);

        for (int i = 0; i < 20; i++) {
            assertThat(size.roll(random)).isEqualTo(1);
        }
    }

    @Test
    void rangeRollsWithinBoundsInclusive() {
        EncounterSize size = new EncounterSize(2, 3);
        Random random = new Random(1);

        boolean sawTwo = false;
        boolean sawThree = false;
        for (int i = 0; i < 200; i++) {
            int rolled = size.roll(random);
            assertThat(rolled).isBetween(2, 3);
            sawTwo |= rolled == 2;
            sawThree |= rolled == 3;
        }
        assertThat(sawTwo).as("saw the minimum").isTrue();
        assertThat(sawThree).as("saw the maximum").isTrue();
    }

    @Test
    void nonPositiveMinThrows() {
        assertThatThrownBy(() -> new EncounterSize(0, 3)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void maxBelowMinThrows() {
        assertThatThrownBy(() -> new EncounterSize(3, 1)).isInstanceOf(IllegalArgumentException.class);
    }
}
