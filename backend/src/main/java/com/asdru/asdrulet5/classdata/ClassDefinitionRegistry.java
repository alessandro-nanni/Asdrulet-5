package com.asdru.asdrulet5.classdata;

import com.asdru.asdrulet5.classdata.classes.BerserkerClassDefinition;
import com.asdru.asdrulet5.classdata.classes.HealerClassDefinition;
import com.asdru.asdrulet5.classdata.classes.MageClassDefinition;
import com.asdru.asdrulet5.classdata.classes.PaladinClassDefinition;
import com.asdru.asdrulet5.classdata.domain.ClassDefinition;
import com.asdru.asdrulet5.classdata.exception.UnknownClassDefinitionException;
import com.asdru.asdrulet5.party.domain.CharacterClass;
import org.springframework.beans.factory.annotation.Value;
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
 * {@code classdata.classes}, and add it to {@link #buildDefinitions}
 * — nothing else needs to change.
 */
@Component
public class ClassDefinitionRegistry {

    private final Map<CharacterClass, ClassDefinition> definitions;

    public ClassDefinitionRegistry(@Value("${app.dev-tools.enabled:false}") boolean devToolsEnabled) {
        this.definitions = buildDefinitions(devToolsEnabled);
    }

    private static Map<CharacterClass, ClassDefinition> buildDefinitions(boolean devToolsEnabled) {
        return Stream.of(
                        HealerClassDefinition.define(),
                        PaladinClassDefinition.define(),
                        BerserkerClassDefinition.define(devToolsEnabled),
                        MageClassDefinition.define())
                .collect(Collectors.toMap(ClassDefinition::characterClass, Function.identity(),
                        (a, b) -> {
                            throw new IllegalStateException("duplicate class definition");
                        },
                        () -> new EnumMap<>(CharacterClass.class)));
    }

    public List<ClassDefinition> all() {
        return List.copyOf(definitions.values());
    }

    public ClassDefinition get(CharacterClass characterClass) {
        ClassDefinition definition = definitions.get(characterClass);
        if (definition == null) {
            throw new UnknownClassDefinitionException(characterClass);
        }
        return definition;
    }
}
