package com.asdru.asdrulet5.enemydata;

import com.asdru.asdrulet5.classdata.domain.AbilityEffect;
import com.asdru.asdrulet5.classdata.domain.Stats;
import com.asdru.asdrulet5.enemydata.domain.EnemyDefinition;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Static catalog of enemy definitions. To add a new enemy: add a factory
 * method here and add it to {@link #buildDefinitions()}.
 */
@Component
public class EnemyDefinitionRegistry {

    public static final String DEFAULT_ENEMY_ID = "goblin-marauder";

    private static final Map<String, EnemyDefinition> DEFINITIONS = buildDefinitions();

    private static Map<String, EnemyDefinition> buildDefinitions() {
        return Stream.of(goblinMarauder())
                .collect(Collectors.toMap(EnemyDefinition::id, Function.identity()));
    }

    private static EnemyDefinition goblinMarauder() {
        return new EnemyDefinition(
                DEFAULT_ENEMY_ID,
                "Goblin Marauder",
                new Stats(220, 15, 8, 0),
                "Rusty Cleaver",
                "Swings a notched blade at whoever's closest.",
                "15 damage",
                AbilityEffect.damage(15)
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
