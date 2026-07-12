package com.asdru.asdrulet5.enemydata;

import com.asdru.asdrulet5.enemydata.domain.EnemyDefinition;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EnemyDefinitionRegistryTest {

    private final EnemyDefinitionRegistry registry = new EnemyDefinitionRegistry();

    @Test
    void allReturnsAtLeastOneDefinition() {
        assertThat(registry.all()).isNotEmpty();
    }

    @Test
    void defaultEnemyIsRegistered() {
        EnemyDefinition definition = registry.get(EnemyDefinitionRegistry.DEFAULT_ENEMY_ID);

        assertThat(definition.stats().maxHealth()).isPositive();
        assertThat(definition.attackEffectSummary()).isNotBlank();
        assertThat(definition.attackEffect()).isNotNull();
    }

    @Test
    void unknownIdThrows() {
        assertThatThrownBy(() -> registry.get("nope"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
