package com.asdru.asdrulet5.classdata.skilltrees;

import com.asdru.asdrulet5.classdata.domain.*;
import com.asdru.asdrulet5.party.domain.CharacterClass;
import lombok.experimental.UtilityClass;

import java.util.List;

@UtilityClass
public class BerserkerSkillTree {

    private static final double CRIT_CHANCE = 0.2;

    public SkillTree define() {
        return new SkillTree(CharacterClass.BERSERKER, List.of(
                new SkillNode("berserker.bloodlust", "Bloodlust",
                        "Sink deeper into the frenzy — Reckless Strike hits harder.", 2, null,
                        new UpgradeAbility(new BasicAbility(
                                "berserker.reckless-strike", "Reckless Strike",
                                "A wild swing with a chance to land a brutal blow.",
                                "16 damage, 20% chance to double",
                                TargetType.SINGLE_ENEMY, 30,
                                AbilityEffect.critDamage(16, CRIT_CHANCE)))),

                new SkillNode("berserker.deeper-wounds", "Deeper Wounds",
                        "Every opening becomes a killing blow more often.", 3, "berserker.bloodlust",
                        new UpgradeAbility(new BasicAbility(
                                "berserker.reckless-strike", "Reckless Strike",
                                "A wild swing with a chance to land a brutal blow.",
                                "16 damage, 35% chance to double",
                                TargetType.SINGLE_ENEMY, 30,
                                AbilityEffect.critDamage(16, 0.35)))),
                new SkillNode("berserker.frenzy", "Frenzy",
                        "Abandon restraint entirely, lashing out again and again.", 5, "berserker.deeper-wounds",
                        new AddAbility(new BasicAbility(
                                "berserker.frenzy", "Frenzy",
                                "A flurry of quick, reckless cuts.",
                                "3 hits of 6 damage",
                                TargetType.SINGLE_ENEMY, 20,
                                AbilityEffect.multiHitDamage(3, 6)))),

                new SkillNode("berserker.rallying-cry", "Rallying Cry",
                        "Battle Fury now roars louder, and lasts longer.", 3, "berserker.bloodlust",
                        new UpgradeAbility(new BasicAbility(
                                "berserker.battle-fury", "Battle Fury",
                                "Rallies an ally into a battle-frenzy.",
                                "+10% damage for 4 turns",
                                TargetType.SINGLE_ALLY, 40,
                                AbilityEffect.applyEffect(() -> ActiveEffect.strength("Strength", "strength", 10, 4))))),
                new SkillNode("berserker.unbreakable", "Unbreakable",
                        "Turns raw fury inward, hardening flesh like iron.", 5, "berserker.rallying-cry",
                        new AddAbility(new BasicAbility(
                                "berserker.unbreakable", "Unbreakable",
                                "Braces against everything the enemy has left.",
                                "+15 defense for 2 turns",
                                TargetType.SELF, 20,
                                AbilityEffect.buffDefense("Unbreakable", "shield", 15, 2))))
        ));
    }
}
