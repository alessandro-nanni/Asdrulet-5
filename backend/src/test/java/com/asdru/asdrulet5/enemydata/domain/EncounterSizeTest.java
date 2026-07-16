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

    @Test
    void scaledForPartySizeIsUnchangedAtBaseline() {
        EncounterSize size = new EncounterSize(2, 3);

        assertThat(size.scaledForPartySize(EncounterSize.BASELINE_PARTY_SIZE)).isEqualTo(new EncounterSize(2, 3));
    }

    @Test
    void scaledForPartySizeAddsOneEnemyPerExtraPlayer() {
        EncounterSize size = new EncounterSize(2, 3);

        assertThat(size.scaledForPartySize(2)).isEqualTo(new EncounterSize(3, 4));
        assertThat(size.scaledForPartySize(4)).isEqualTo(new EncounterSize(5, 6));
    }

    @Test
    void scaledForPartySizeBelowBaselineNeverShrinksBelowBaseline() {
        EncounterSize size = new EncounterSize(2, 3);

        assertThat(size.scaledForPartySize(0)).isEqualTo(new EncounterSize(2, 3));
    }
}
