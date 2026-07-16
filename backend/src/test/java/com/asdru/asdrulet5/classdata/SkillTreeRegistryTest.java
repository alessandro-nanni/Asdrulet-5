package com.asdru.asdrulet5.classdata;

import com.asdru.asdrulet5.classdata.domain.*;
import com.asdru.asdrulet5.party.domain.CharacterClass;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SkillTreeRegistryTest {

    private final SkillTreeRegistry skillTreeRegistry = new SkillTreeRegistry();
    private final ClassDefinitionRegistry classDefinitionRegistry = new ClassDefinitionRegistry(false);

    @Test
    void allReturnsExactlyOneTreePerCharacterClass() {
        assertThat(skillTreeRegistry.all())
                .extracting(SkillTree::characterClass)
                .containsExactlyInAnyOrderElementsOf(List.of(CharacterClass.values()));
    }

    @ParameterizedTest
    @EnumSource(CharacterClass.class)
    void everyNodeIdIsUniqueWithinItsTree(CharacterClass characterClass) {
        SkillTree tree = skillTreeRegistry.get(characterClass);

        Set<String> ids = new HashSet<>();
        for (SkillNode node : tree.nodes()) {
            assertThat(ids.add(node.id())).as("duplicate node id " + node.id()).isTrue();
        }
    }

    @ParameterizedTest
    @EnumSource(CharacterClass.class)
    void everyNonRootParentIdResolvesToARealNodeInTheSameTree(CharacterClass characterClass) {
        SkillTree tree = skillTreeRegistry.get(characterClass);

        for (SkillNode node : tree.nodes()) {
            if (node.parentId() != null) {
                assertThat(tree.nodeById(node.parentId())).isNotNull();
            }
        }
    }

    @ParameterizedTest
    @EnumSource(CharacterClass.class)
    void everyTreeHasAtLeastOneRootNode(CharacterClass characterClass) {
        SkillTree tree = skillTreeRegistry.get(characterClass);

        assertThat(tree.nodes()).anyMatch(node -> node.parentId() == null);
    }

    @ParameterizedTest
    @EnumSource(CharacterClass.class)
    void everyUpgradeAbilityReplacesAnIdTheClassAlreadyHas(CharacterClass characterClass) {
        ClassDefinition definition = classDefinitionRegistry.get(characterClass);
        SkillTree tree = skillTreeRegistry.get(characterClass);

        Set<String> knownIds = new HashSet<>();
        definition.abilities().forEach(ability -> knownIds.add(ability.id()));
        tree.nodes().stream()
                .filter(node -> node.effect() instanceof AddAbility)
                .map(node -> ((AddAbility) node.effect()).newAbility().id())
                .forEach(knownIds::add);

        for (SkillNode node : tree.nodes()) {
            if (node.effect() instanceof UpgradeAbility upgrade) {
                assertThat(knownIds).as(node.id() + " upgrades an unknown ability").contains(upgrade.replacement().id());
            }
        }
    }

    @ParameterizedTest
    @EnumSource(CharacterClass.class)
    void everyAddAbilityIdIsNewToTheClass(CharacterClass characterClass) {
        ClassDefinition definition = classDefinitionRegistry.get(characterClass);
        SkillTree tree = skillTreeRegistry.get(characterClass);

        Set<String> baseIds = new HashSet<>();
        definition.abilities().forEach(ability -> baseIds.add(ability.id()));

        Set<String> addedIds = new HashSet<>();
        for (SkillNode node : tree.nodes()) {
            if (node.effect() instanceof AddAbility add) {
                String id = add.newAbility().id();
                assertThat(baseIds).as(node.id() + " adds an id the class already has").doesNotContain(id);
                assertThat(addedIds.add(id)).as(node.id() + " adds a duplicate id " + id).isTrue();
            }
        }
    }
}
