package com.asdru.asdrulet5.enemydata;

import com.asdru.asdrulet5.classdata.domain.AbilityEffect;
import com.asdru.asdrulet5.classdata.domain.BasicAbility;
import com.asdru.asdrulet5.classdata.domain.Stats;
import com.asdru.asdrulet5.classdata.domain.TargetType;
import com.asdru.asdrulet5.enemydata.domain.EnemyDefinition;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Static catalog of enemy definitions. To add a new enemy: add a factory
 * method here and add it to {@link #buildDefinitions()} — then give it an
 * entry in {@code EnemyEncounterRegistry} to actually make it spawnable
 * anywhere (this registry just describes what an enemy is, the same way
 * {@code ItemDefinitionRegistry} does for items). Every enemy currently
 * defines exactly one {@link BasicAbility} as its "attack" — see
 * {@code Combat.resolveEnemyTurn}, which always uses an enemy's first
 * ability for now, ahead of real move-selection AI for enemies with more
 * than one.
 *
 * <p>{@link #DEFAULT_ENEMY_ID} (Goblin Marauder) is reserved for boss
 * encounters — see {@code EnemyEncounterRegistry}'s own doc — the other
 * enemies here are the regular floor-1 pool, individually weaker since 2-3
 * of them show up together instead of one at a time.
 */
@Component
public class EnemyDefinitionRegistry {

    public static final String DEFAULT_ENEMY_ID = "goblin-marauder";

    private static final Map<String, EnemyDefinition> DEFINITIONS = buildDefinitions();

    private static Map<String, EnemyDefinition> buildDefinitions() {
        return Stream.of(goblinMarauder(), caveRat(), goblinSkirmisher(), banditThug())
                .collect(Collectors.toMap(EnemyDefinition::id, Function.identity()));
    }

    private static EnemyDefinition goblinMarauder() {
        return new EnemyDefinition(
                DEFAULT_ENEMY_ID,
                "Goblin Marauder",
                new Stats(220, 8, 0),
                List.of(new BasicAbility(
                        "goblin-marauder.rusty-cleaver", "Rusty Cleaver",
                        "Swings a notched blade at whoever's closest.", "15 damage",
                        TargetType.SINGLE_ENEMY, 0, AbilityEffect.damage(15))),
                List.of()
        );
    }

    private static EnemyDefinition caveRat() {
        return new EnemyDefinition(
                "cave-rat",
                "Cave Rat",
                new Stats(60, 1, 0),
                List.of(new BasicAbility(
                        "cave-rat.gnawing-bite", "Gnawing Bite",
                        "Latches on and gnaws with needle-sharp teeth.", "8 damage",
                        TargetType.SINGLE_ENEMY, 0, AbilityEffect.damage(8))),
                List.of()
        );
    }

    private static EnemyDefinition goblinSkirmisher() {
        return new EnemyDefinition(
                "goblin-skirmisher",
                "Goblin Skirmisher",
                new Stats(90, 4, 0),
                List.of(new BasicAbility(
                        "goblin-skirmisher.jagged-dagger", "Jagged Dagger",
                        "Darts in for a quick stab, then dances back.", "10 damage",
                        TargetType.SINGLE_ENEMY, 0, AbilityEffect.damage(10))),
                List.of()
        );
    }

    private static EnemyDefinition banditThug() {
        return new EnemyDefinition(
                "bandit-thug",
                "Bandit Thug",
                new Stats(110, 6, 0),
                List.of(new BasicAbility(
                        "bandit-thug.club-swing", "Club Swing",
                        "A clumsy but heavy overhead swing.", "12 damage",
                        TargetType.SINGLE_ENEMY, 0, AbilityEffect.damage(12))),
                List.of()
        );
    }

    public List<EnemyDefinition> all() {
        return List.copyOf(DEFINITIONS.values());
    }

    public EnemyDefinition get(String id) {
        EnemyDefinition definition = DEFINITIONS.get(id);
        if (definition == null) {
            throw new IllegalArgumentException("No enemy definition registered for " + id);
        }
        return definition;
    }
}
