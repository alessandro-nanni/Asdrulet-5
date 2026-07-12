package com.asdru.asdrulet5.classdata;

import com.asdru.asdrulet5.classdata.domain.BasicAbility;
import com.asdru.asdrulet5.classdata.domain.ClassDefinition;
import com.asdru.asdrulet5.classdata.domain.UltimateAbility;
import com.asdru.asdrulet5.party.domain.CharacterClass;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ClassDefinitionRegistryTest {

    private final ClassDefinitionRegistry registry = new ClassDefinitionRegistry();

    @Test
    void allReturnsExactlyOneDefinitionPerCharacterClass() {
        assertThat(registry.all())
                .extracting(ClassDefinition::characterClass)
                .containsExactlyInAnyOrderElementsOf(List.of(CharacterClass.values()));
    }

    @ParameterizedTest
    @EnumSource(CharacterClass.class)
    void everyClassHasAtLeastTwoBasicsAndExactlyOneUltimate(CharacterClass characterClass) {
        ClassDefinition definition = registry.get(characterClass);

        long basicCount = definition.abilities().stream().filter(a -> a instanceof BasicAbility).count();
        long ultimateCount = definition.abilities().stream().filter(a -> a instanceof UltimateAbility).count();

        assertThat(basicCount).isGreaterThanOrEqualTo(2);
        assertThat(ultimateCount).isEqualTo(1);
    }

    @ParameterizedTest
    @EnumSource(CharacterClass.class)
    void everyClassHasPositiveHealthAndSpeed(CharacterClass characterClass) {
        ClassDefinition definition = registry.get(characterClass);

        assertThat(definition.stats().maxHealth()).isPositive();
    }
}
