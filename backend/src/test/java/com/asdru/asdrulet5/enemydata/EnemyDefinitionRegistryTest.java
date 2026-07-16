package com.asdru.asdrulet5.enemydata;

import com.asdru.asdrulet5.classdata.domain.BasicAbility;
import com.asdru.asdrulet5.enemydata.domain.EnemyDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;

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
        assertThat(definition.abilities()).isNotEmpty();
        assertThat(definition.abilities().getFirst().effectSummary()).isNotBlank();
        assertThat(definition.abilities().getFirst().effect()).isNotNull();
    }

    @Test
    void regularFloorOnePoolEnemiesAreRegistered() {
        for (String enemyId : List.of("cave-rat", "goblin-skirmisher", "bandit-thug")) {
            EnemyDefinition definition = registry.get(enemyId);
            assertThat(definition.stats().maxHealth()).isPositive();
            assertThat(definition.abilities()).isNotEmpty();
        }
    }

    /**
     * Every enemy needs more than one move for {@code Combat.chooseEnemyAbility}'s
     * affordability-gated preference order to have anything real to choose
     * between, and a guaranteed-affordable (0-stamina-cost) last move so it's
     * never left with nothing it can do once its stamina budget runs dry.
     */
    @Test
    void everyEnemyHasMultipleAbilitiesEndingInAZeroCostFallback() {
        for (EnemyDefinition definition : registry.all()) {
            assertThat(definition.abilities()).as(definition.id()).hasSizeGreaterThan(1);
            assertThat(definition.abilities().getLast()).as(definition.id())
                    .isInstanceOfSatisfying(BasicAbility.class,
                            fallback -> assertThat(fallback.staminaCost()).isZero());
        }
    }

    @Test
    void bossHasADeeperMoveSetThanARegularEnemy() {
        EnemyDefinition boss = registry.get(EnemyDefinitionRegistry.DEFAULT_ENEMY_ID);
        EnemyDefinition regular = registry.get("cave-rat");

        assertThat(boss.abilities().size()).isGreaterThan(regular.abilities().size());
    }

    @Test
    void unknownIdThrows() {
        assertThatThrownBy(() -> registry.get("nope"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
