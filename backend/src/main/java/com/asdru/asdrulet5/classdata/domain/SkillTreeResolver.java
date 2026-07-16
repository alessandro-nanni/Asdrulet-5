package com.asdru.asdrulet5.classdata.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Turns a member's set of unlocked {@link SkillNode} ids into their actual
 * effective ability list — the one seam combat.CombatService needs instead
 * of a class's raw, unmodified {@link ClassDefinition#abilities()}.
 */
public final class SkillTreeResolver {

    private SkillTreeResolver() {
    }

    /**
     * Starts from the class's base abilities (keyed by id), then applies
     * every unlocked node in shallowest-to-deepest order — so a deeper
     * upgrade for the same ability id always wins over an earlier tier's,
     * regardless of the order nodes happen to be declared in {@code tree}.
     * Trusts {@code unlockedNodeIds} as already valid: prerequisite and mana
     * checks happen at unlock time (see party.domain.Party#unlockSkill), not
     * here.
     */
    public static List<Ability> effectiveAbilities(ClassDefinition definition, SkillTree tree, Set<String> unlockedNodeIds) {
        Map<String, Ability> byId = new LinkedHashMap<>();
        for (Ability ability : definition.abilities()) {
            byId.put(ability.id(), ability);
        }

        List<Ability> added = new ArrayList<>();
        List<SkillNode> unlockedInOrder = tree.nodes().stream()
                .filter(node -> unlockedNodeIds.contains(node.id()))
                .sorted(Comparator.comparingInt(node -> depthOf(node, tree)))
                .toList();

        for (SkillNode node : unlockedInOrder) {
            switch (node.effect()) {
                case UpgradeAbility upgrade -> byId.put(upgrade.replacement().id(), upgrade.replacement());
                case AddAbility add -> added.add(add.newAbility());
            }
        }

        List<Ability> result = new ArrayList<>(byId.values());
        result.addAll(added);
        return List.copyOf(result);
    }

    private static int depthOf(SkillNode node, SkillTree tree) {
        int depth = 0;
        SkillNode current = node;
        while (current.parentId() != null) {
            current = tree.nodeById(current.parentId());
            depth++;
        }
        return depth;
    }
}
