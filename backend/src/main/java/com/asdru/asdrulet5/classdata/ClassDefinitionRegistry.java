package com.asdru.asdrulet5.classdata;

import com.asdru.asdrulet5.classdata.classes.HealerClassDefinition;
import com.asdru.asdrulet5.classdata.classes.MageClassDefinition;
import com.asdru.asdrulet5.classdata.classes.TankClassDefinition;
import com.asdru.asdrulet5.classdata.classes.WarriorClassDefinition;
import com.asdru.asdrulet5.classdata.domain.ClassDefinition;
import com.asdru.asdrulet5.classdata.exception.UnknownClassDefinitionException;
import com.asdru.asdrulet5.party.domain.CharacterClass;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Static catalog of per-class stats and abilities. To add a new class: add
 * the enum constant to {@link CharacterClass}, add a definition class under
 * {@code classdata.classes}, and add it to {@link #buildDefinitions()} —
 * nothing else needs to change.
 */
@Component
public class ClassDefinitionRegistry {

    private static final Map<CharacterClass, ClassDefinition> DEFINITIONS = buildDefinitions();

    private static Map<CharacterClass, ClassDefinition> buildDefinitions() {
        return Stream.of(
                        HealerClassDefinition.define(),
                        TankClassDefinition.define(),
                        WarriorClassDefinition.define(),
                        MageClassDefinition.define())
                .collect(Collectors.toMap(ClassDefinition::characterClass, Function.identity(),
                        (a, b) -> {
                            throw new IllegalStateException("duplicate class definition");
                        },
                        () -> new EnumMap<>(CharacterClass.class)));
    }

    public List<ClassDefinition> all() {
        return List.copyOf(DEFINITIONS.values());
    }

    public ClassDefinition get(CharacterClass characterClass) {
        ClassDefinition definition = DEFINITIONS.get(characterClass);
        if (definition == null) {
            throw new UnknownClassDefinitionException(characterClass);
        }
        return definition;
    }
}
