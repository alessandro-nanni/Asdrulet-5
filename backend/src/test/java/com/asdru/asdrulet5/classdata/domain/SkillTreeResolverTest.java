package com.asdru.asdrulet5.classdata.domain;

import com.asdru.asdrulet5.party.domain.CharacterClass;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SkillTreeResolverTest {

    private static final BasicAbility STRIKE = new BasicAbility(
            "test.strike", "Strike", "A basic attack.", "10 damage", TargetType.SINGLE_ENEMY, 10,
            AbilityEffect.damage(10));

    private static final UltimateAbility ULTIMATE = new UltimateAbility(
            "test.ultimate", "Ultimate", "A big attack.", "50 damage", TargetType.SINGLE_ENEMY, 100,
            AbilityEffect.damage(50));

    private static final ClassDefinition DEFINITION = new ClassDefinition(
            CharacterClass.BERSERKER, "Test", "A test class.", new Stats(100, 5, 100),
            List.of(STRIKE, new BasicAbility(
                    "test.strike-2", "Strike 2", "Another basic attack.", "10 damage", TargetType.SINGLE_ENEMY, 10,
                    AbilityEffect.damage(10)), ULTIMATE));

    private static final BasicAbility STRIKE_TIER_1 = new BasicAbility(
            "test.strike", "Strike", "A basic attack.", "15 damage", TargetType.SINGLE_ENEMY, 10,
            AbilityEffect.damage(15));

    private static final BasicAbility STRIKE_TIER_2 = new BasicAbility(
            "test.strike", "Strike", "A basic attack.", "20 damage", TargetType.SINGLE_ENEMY, 10,
            AbilityEffect.damage(20));

    private static final BasicAbility NEW_ABILITY = new BasicAbility(
            "test.new-ability", "New Ability", "A brand new move.", "5 damage", TargetType.SINGLE_ENEMY, 5,
            AbilityEffect.damage(5));

    private static SkillNode node(String id, String parentId, SkillNodeEffect effect) {
        return new SkillNode(id, id, "A test node.", 2, parentId, effect);
    }

    @Test
    void noUnlocksLeavesAbilitiesUnchanged() {
        SkillTree tree = new SkillTree(CharacterClass.BERSERKER, List.of(
                node("root", null, new UpgradeAbility(STRIKE_TIER_1))));

        List<Ability> result = SkillTreeResolver.effectiveAbilities(DEFINITION, tree, Set.of());

        assertThat(result).containsExactlyElementsOf(DEFINITION.abilities());
    }

    @Test
    void unlockingAChainOfUpgradesAppliesTheDeepestOne() {
        SkillTree tree = new SkillTree(CharacterClass.BERSERKER, List.of(
                node("root", null, new UpgradeAbility(STRIKE_TIER_1)),
                node("mid", "root", new UpgradeAbility(STRIKE_TIER_2))));

        List<Ability> result = SkillTreeResolver.effectiveAbilities(
                DEFINITION, tree, Set.of("root", "mid"));

        Ability resolvedStrike = result.stream().filter(a -> a.id().equals("test.strike")).findFirst().orElseThrow();
        assertThat(resolvedStrike.effectSummary()).isEqualTo("20 damage");
    }

    @Test
    void resolutionIsOrderIndependentOfDeclarationOrder() {
        // Deliberately declared deepest-first — the resolver must still apply
        // "mid" last since it sorts by actual tree depth, not list order.
        SkillTree tree = new SkillTree(CharacterClass.BERSERKER, List.of(
                node("mid", "root", new UpgradeAbility(STRIKE_TIER_2)),
                node("root", null, new UpgradeAbility(STRIKE_TIER_1))));

        List<Ability> result = SkillTreeResolver.effectiveAbilities(
                DEFINITION, tree, Set.of("root", "mid"));

        Ability resolvedStrike = result.stream().filter(a -> a.id().equals("test.strike")).findFirst().orElseThrow();
        assertThat(resolvedStrike.effectSummary()).isEqualTo("20 damage");
    }

    @Test
    void onlyUnlockingTheRootLeavesTheMidTierUpgradeUnapplied() {
        SkillTree tree = new SkillTree(CharacterClass.BERSERKER, List.of(
                node("root", null, new UpgradeAbility(STRIKE_TIER_1)),
                node("mid", "root", new UpgradeAbility(STRIKE_TIER_2))));

        List<Ability> result = SkillTreeResolver.effectiveAbilities(DEFINITION, tree, Set.of("root"));

        Ability resolvedStrike = result.stream().filter(a -> a.id().equals("test.strike")).findFirst().orElseThrow();
        assertThat(resolvedStrike.effectSummary()).isEqualTo("15 damage");
    }

    @Test
    void addAbilityNodeAppendsWithoutDisturbingExistingIds() {
        SkillTree tree = new SkillTree(CharacterClass.BERSERKER, List.of(
                node("root", null, new UpgradeAbility(STRIKE_TIER_1)),
                node("capstone", "root", new AddAbility(NEW_ABILITY))));

        List<Ability> result = SkillTreeResolver.effectiveAbilities(
                DEFINITION, tree, Set.of("root", "capstone"));

        assertThat(result).hasSize(DEFINITION.abilities().size() + 1);
        assertThat(result).extracting(Ability::id).contains("test.new-ability");
    }
}
