package com.asdru.asdrulet5.classdata.domain;

import com.asdru.asdrulet5.party.domain.CharacterClass;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClassDefinitionTest {

    private static final Stats STATS = new Stats(100, 10, 10, 100);

    private static final AbilityEffect DAMAGE_EFFECT = new DamageEffect(10);

    private static BasicAbility basic(String id) {
        return new BasicAbility(id, "Basic " + id, "A basic ability.", TargetType.SINGLE_ENEMY, 10, DAMAGE_EFFECT);
    }

    private static UltimateAbility ultimate(String id) {
        return new UltimateAbility(id, "Ultimate " + id, "An ultimate ability.", TargetType.SINGLE_ENEMY, 100, DAMAGE_EFFECT);
    }

    @Test
    void constructingWithNoUltimateThrows() {
        assertThatThrownBy(() -> new ClassDefinition(
                CharacterClass.MAGE, "Mage", "flavor", STATS, List.of(basic("a"), basic("b"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exactly one UltimateAbility");
    }

    @Test
    void constructingWithTwoUltimatesThrows() {
        assertThatThrownBy(() -> new ClassDefinition(
                CharacterClass.MAGE, "Mage", "flavor", STATS,
                List.of(basic("a"), basic("b"), ultimate("u1"), ultimate("u2"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exactly one UltimateAbility");
    }

    @Test
    void constructingWithFewerThanTwoBasicsThrows() {
        assertThatThrownBy(() -> new ClassDefinition(
                CharacterClass.MAGE, "Mage", "flavor", STATS, List.of(basic("a"), ultimate("u"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least two BasicAbility");
    }

    @Test
    void constructingWithValidAbilitiesSucceeds() {
        assertThatCode(() -> new ClassDefinition(
                CharacterClass.MAGE, "Mage", "flavor", STATS,
                List.of(basic("a"), basic("b"), ultimate("u"))))
                .doesNotThrowAnyException();
    }
}
