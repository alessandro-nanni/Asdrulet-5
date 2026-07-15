package com.asdru.asdrulet5.classdata.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DamageTest {

    @Test
    void ofBuildsANonCriticalDamage() {
        Damage damage = Damage.of(15);

        assertThat(damage.amount()).isEqualTo(15);
        assertThat(damage.critical()).isFalse();
    }

    @Test
    void negativeAmountThrows() {
        assertThatThrownBy(() -> new Damage(-1, false)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void zeroAmountIsAllowed() {
        assertThat(new Damage(0, false).amount()).isZero();
    }
}
