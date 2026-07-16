package com.asdru.asdrulet5.classdata.skilltrees;

import com.asdru.asdrulet5.classdata.domain.*;
import com.asdru.asdrulet5.party.domain.CharacterClass;
import lombok.experimental.UtilityClass;

import java.util.List;

@UtilityClass
public class PaladinSkillTree {

    public SkillTree define() {
        return new SkillTree(CharacterClass.PALADIN, List.of(
                new SkillNode("paladin.aegis-training", "Aegis Training",
                        "A heavier shield, a harder slam.", 2, null,
                        new UpgradeAbility(new BasicAbility(
                                "paladin.shield-slam", "Shield Slam",
                                "Slams a foe with a shield, daring them to strike back.",
                                "8 damage + Taunt for 3 turns",
                                TargetType.SINGLE_ENEMY, 25,
                                AbilityEffect.damageAndApplyEffect(8,
                                        actor -> ActiveEffect.taunt("Taunt", "taunt", 3, actor.combatantId(), actor.displayName()))))),

                new SkillNode("paladin.fortified-stance", "Fortified Stance",
                        "Protect braces against far more, for far longer.", 3, "paladin.aegis-training",
                        new UpgradeAbility(new BasicAbility(
                                "paladin.protect", "Protect",
                                "Raises a defensive stance, bracing for impact.",
                                "+20 defense for 3 turns",
                                TargetType.SELF, 40,
                                AbilityEffect.buffDefense("Protection", "shield", 20, 3)))),
                new SkillNode("paladin.aegis", "Aegis",
                        "Extends the same protection to a struggling ally.", 5, "paladin.fortified-stance",
                        new AddAbility(new BasicAbility(
                                "paladin.aegis", "Aegis",
                                "Shares a fraction of the Paladin's own resilience with an ally.",
                                "+15 defense for 2 turns",
                                TargetType.SINGLE_ALLY, 30,
                                AbilityEffect.buffDefense("Aegis", "shield", 15, 2)))),
                new SkillNode("paladin.greater-aegis", "Greater Aegis",
                        "Shares even more of the Paladin's own resilience.", 8, "paladin.aegis",
                        new UpgradeAbility(new BasicAbility(
                                "paladin.aegis", "Aegis",
                                "Shares a fraction of the Paladin's own resilience with an ally.",
                                "+22 defense for 3 turns",
                                TargetType.SINGLE_ALLY, 30,
                                AbilityEffect.buffDefense("Aegis", "shield", 22, 3)))),
                new SkillNode("paladin.bastion", "Bastion",
                        "Turns the whole party into a wall of steel.", 12, "paladin.greater-aegis",
                        new AddAbility(new BasicAbility(
                                "paladin.bastion", "Bastion",
                                "A rampart of borrowed resilience shared with everyone.",
                                "+12 defense for 2 turns, to all allies",
                                TargetType.ALL_ALLIES, 55,
                                AbilityEffect.buffDefense("Bastion", "shield", 12, 2)))),
                new SkillNode("paladin.guardians-rebuke", "Guardian's Rebuke",
                        "Punishes whoever dares strike a protected ally.", 12, "paladin.greater-aegis",
                        new AddAbility(new BasicAbility(
                                "paladin.guardians-rebuke", "Guardian's Rebuke",
                                "A sharp reprimand for anyone who tests the Paladin's shield.",
                                "10 damage + Taunt for 2 turns",
                                TargetType.SINGLE_ENEMY, 35,
                                AbilityEffect.damageAndApplyEffect(10,
                                        actor -> ActiveEffect.taunt("Taunt", "taunt", 2, actor.combatantId(), actor.displayName()))))),

                new SkillNode("paladin.provoke", "Provoke",
                        "Shield Slam draws the enemy's ire for far longer.", 3, "paladin.aegis-training",
                        new UpgradeAbility(new BasicAbility(
                                "paladin.shield-slam", "Shield Slam",
                                "Slams a foe with a shield, daring them to strike back.",
                                "8 damage + Taunt for 4 turns",
                                TargetType.SINGLE_ENEMY, 25,
                                AbilityEffect.damageAndApplyEffect(8,
                                        actor -> ActiveEffect.taunt("Taunt", "taunt", 4, actor.combatantId(), actor.displayName()))))),
                new SkillNode("paladin.holy-retribution", "Holy Retribution",
                        "Answers every provocation with judgment on the whole line.", 5, "paladin.provoke",
                        new AddAbility(new BasicAbility(
                                "paladin.holy-retribution", "Holy Retribution",
                                "A wave of righteous force crashing over every foe.",
                                "10 damage to all enemies",
                                TargetType.ALL_ENEMIES, 50,
                                AbilityEffect.damage(10)))),
                new SkillNode("paladin.righteous-fury", "Righteous Fury",
                        "Retribution burns hotter the longer the Paladin holds the line.", 8, "paladin.holy-retribution",
                        new UpgradeAbility(new BasicAbility(
                                "paladin.holy-retribution", "Holy Retribution",
                                "A wave of righteous force crashing over every foe.",
                                "15 damage to all enemies",
                                TargetType.ALL_ENEMIES, 50,
                                AbilityEffect.damage(15)))),
                new SkillNode("paladin.judgment-slam", "Judgment Slam",
                        "A single, devastating blow that still dares them to answer.", 12, "paladin.righteous-fury",
                        new AddAbility(new BasicAbility(
                                "paladin.judgment-slam", "Judgment Slam",
                                "A crushing blow backed by the full weight of righteous fury.",
                                "18 damage + Taunt for 3 turns",
                                TargetType.SINGLE_ENEMY, 40,
                                AbilityEffect.damageAndApplyEffect(18,
                                        actor -> ActiveEffect.taunt("Taunt", "taunt", 3, actor.combatantId(), actor.displayName()))))),
                new SkillNode("paladin.last-stand", "Last Stand",
                        "When the line must hold, it holds absolutely.", 12, "paladin.righteous-fury",
                        new AddAbility(new BasicAbility(
                                "paladin.last-stand", "Last Stand",
                                "Plants the Paladin's feet and refuses to yield.",
                                "+25 defense for 3 turns",
                                TargetType.SELF, 40,
                                AbilityEffect.buffDefense("Last Stand", "shield", 25, 3))))
        ));
    }
}
