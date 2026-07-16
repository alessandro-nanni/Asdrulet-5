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
                new SkillNode("berserker.bloodletting", "Bloodletting",
                        "Every strike opens the wound wider.", 8, "berserker.frenzy",
                        new UpgradeAbility(new BasicAbility(
                                "berserker.frenzy", "Frenzy",
                                "A flurry of quick, reckless cuts.",
                                "4 hits of 8 damage",
                                TargetType.SINGLE_ENEMY, 20,
                                AbilityEffect.multiHitDamage(4, 8)))),
                new SkillNode("berserker.whirlwind", "Whirlwind",
                        "No longer just one foe suffers the frenzy.", 12, "berserker.bloodletting",
                        new AddAbility(new BasicAbility(
                                "berserker.whirlwind", "Whirlwind",
                                "A spinning flurry that catches everything nearby.",
                                "2 hits of 7 damage to all enemies",
                                TargetType.ALL_ENEMIES, 40,
                                AbilityEffect.multiHitDamage(2, 7)))),
                new SkillNode("berserker.execute", "Execute",
                        "A killing blow aimed at whatever's already bleeding.", 12, "berserker.bloodletting",
                        new AddAbility(new BasicAbility(
                                "berserker.execute", "Execute",
                                "A single, merciless strike meant to finish the fight.",
                                "20 damage, 40% chance to double",
                                TargetType.SINGLE_ENEMY, 35,
                                AbilityEffect.critDamage(20, 0.4)))),

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
                                AbilityEffect.buffDefense("Unbreakable", "shield", 15, 2)))),
                new SkillNode("berserker.iron-will", "Iron Will",
                        "The frenzy hardens into something that doesn't break.", 8, "berserker.unbreakable",
                        new UpgradeAbility(new BasicAbility(
                                "berserker.unbreakable", "Unbreakable",
                                "Braces against everything the enemy has left.",
                                "+22 defense for 3 turns",
                                TargetType.SELF, 20,
                                AbilityEffect.buffDefense("Unbreakable", "shield", 22, 3)))),
                new SkillNode("berserker.warlords-shout", "Warlord's Shout",
                        "Rallies the whole party behind its toughest fighter.", 12, "berserker.iron-will",
                        new AddAbility(new BasicAbility(
                                "berserker.warlords-shout", "Warlord's Shout",
                                "A battle cry that steels the whole party.",
                                "+10 defense for 2 turns, to all allies",
                                TargetType.ALL_ALLIES, 45,
                                AbilityEffect.buffDefense("Warlord's Shout", "shield", 10, 2)))),
                new SkillNode("berserker.vengeful-strike", "Vengeful Strike",
                        "Answers every hit taken with one twice as hard.", 12, "berserker.iron-will",
                        new AddAbility(new BasicAbility(
                                "berserker.vengeful-strike", "Vengeful Strike",
                                "A brutal counter-attack fueled by every blow endured.",
                                "16 damage, 30% chance to double",
                                TargetType.SINGLE_ENEMY, 35,
                                AbilityEffect.critDamage(16, 0.3))))
        ));
    }
}
