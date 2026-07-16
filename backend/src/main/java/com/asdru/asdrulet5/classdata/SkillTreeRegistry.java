package com.asdru.asdrulet5.classdata;

import com.asdru.asdrulet5.classdata.domain.SkillTree;
import com.asdru.asdrulet5.classdata.exception.UnknownClassDefinitionException;
import com.asdru.asdrulet5.classdata.skilltrees.BerserkerSkillTree;
import com.asdru.asdrulet5.classdata.skilltrees.HealerSkillTree;
import com.asdru.asdrulet5.classdata.skilltrees.MageSkillTree;
import com.asdru.asdrulet5.classdata.skilltrees.PaladinSkillTree;
import com.asdru.asdrulet5.party.domain.CharacterClass;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Static catalog of per-class skill trees — the progression layer on top of
 * {@link ClassDefinitionRegistry}'s base kits. To add a new class's tree: add
 * a definition class under {@code classdata.skilltrees} and add it to
 * {@link #buildTrees} — nothing else needs to change.
 */
@Component
public class SkillTreeRegistry {

    private final Map<CharacterClass, SkillTree> trees;

    public SkillTreeRegistry() {
        this.trees = buildTrees();
    }

    private static Map<CharacterClass, SkillTree> buildTrees() {
        return Stream.of(
                        HealerSkillTree.define(),
                        PaladinSkillTree.define(),
                        BerserkerSkillTree.define(),
                        MageSkillTree.define())
                .collect(Collectors.toMap(SkillTree::characterClass, Function.identity(),
                        (a, b) -> {
                            throw new IllegalStateException("duplicate skill tree");
                        },
                        () -> new EnumMap<>(CharacterClass.class)));
    }

    public List<SkillTree> all() {
        return List.copyOf(trees.values());
    }

    public SkillTree get(CharacterClass characterClass) {
        SkillTree tree = trees.get(characterClass);
        if (tree == null) {
            throw new UnknownClassDefinitionException(characterClass);
        }
        return tree;
    }
}
