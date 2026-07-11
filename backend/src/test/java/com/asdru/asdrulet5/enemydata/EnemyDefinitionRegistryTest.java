package com.asdru.asdrulet5.enemydata;

import com.asdru.asdrulet5.classdata.domain.DamageEffect;
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
        assertThat(definition.attackEffect()).isInstanceOf(DamageEffect.class);
        assertThat(((DamageEffect) definition.attackEffect()).power()).isPositive();
    }

    @Test
    void unknownIdThrows() {
        assertThatThrownBy(() -> registry.get("nope"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
