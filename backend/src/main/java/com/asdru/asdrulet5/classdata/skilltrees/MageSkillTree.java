package com.asdru.asdrulet5.classdata.skilltrees;

import com.asdru.asdrulet5.classdata.domain.*;
import com.asdru.asdrulet5.party.domain.CharacterClass;
import lombok.experimental.UtilityClass;

import java.util.List;

@UtilityClass
public class MageSkillTree {

    public SkillTree define() {
        return new SkillTree(CharacterClass.MAGE, List.of(
                new SkillNode("mage.overcharge", "Overcharge",
                        "Pumps more raw arcane energy into every blast.", 2, null,
                        new UpgradeAbility(new BasicAbility(
                                "mage.arcane-blast", "Arcane Blast",
                                "Detonates arcane energy across every enemy.",
                                "13 damage to all enemies",
                                TargetType.ALL_ENEMIES, 60,
                                AbilityEffect.damage(13)))),

                new SkillNode("mage.amplify", "Amplify",
                        "Arcane Blast detonates with even greater force.", 3, "mage.overcharge",
                        new UpgradeAbility(new BasicAbility(
                                "mage.arcane-blast", "Arcane Blast",
                                "Detonates arcane energy across every enemy.",
                                "18 damage to all enemies",
                                TargetType.ALL_ENEMIES, 60,
                                AbilityEffect.damage(18)))),
                new SkillNode("mage.meteor", "Meteor",
                        "Calls down a single devastating blow on the whole enemy line.", 5, "mage.amplify",
                        new AddAbility(new BasicAbility(
                                "mage.meteor", "Meteor",
                                "A blazing rock torn from the sky itself.",
                                "24 damage to all enemies",
                                TargetType.ALL_ENEMIES, 90,
                                AbilityEffect.damage(24)))),
                new SkillNode("mage.cataclysm", "Cataclysm",
                        "The falling star grows into a falling world.", 8, "mage.meteor",
                        new UpgradeAbility(new BasicAbility(
                                "mage.meteor", "Meteor",
                                "A blazing rock torn from the sky itself.",
                                "32 damage to all enemies",
                                TargetType.ALL_ENEMIES, 90,
                                AbilityEffect.damage(32)))),
                new SkillNode("mage.chain-lightning", "Chain Lightning",
                        "Arcane fire chains from one target to the next.", 12, "mage.cataclysm",
                        new AddAbility(new BasicAbility(
                                "mage.chain-lightning", "Chain Lightning",
                                "A bolt of lightning that arcs across every foe twice over.",
                                "2 hits of 10 damage to all enemies",
                                TargetType.ALL_ENEMIES, 70,
                                AbilityEffect.multiHitDamage(2, 10)))),
                new SkillNode("mage.arcane-nova", "Arcane Nova",
                        "Every hit has a chance to detonate twice over.", 12, "mage.cataclysm",
                        new AddAbility(new BasicAbility(
                                "mage.arcane-nova", "Arcane Nova",
                                "An unstable burst of raw arcane force.",
                                "20 damage to all enemies, 30% chance to double each",
                                TargetType.ALL_ENEMIES, 85,
                                AbilityEffect.critDamage(20, 0.3)))),

                new SkillNode("mage.deep-freeze", "Deep Freeze",
                        "Frost Lock's ice runs colder, and holds far longer.", 3, "mage.overcharge",
                        new UpgradeAbility(new BasicAbility(
                                "mage.frost-lock", "Frost Lock",
                                "Encases an enemy in ice, locking them in place.",
                                "Frozen for 3 turns",
                                TargetType.SINGLE_ENEMY, 80,
                                AbilityEffect.applyEffect(() -> ActiveEffect.frozen("Frozen", "frozen", 3))))),
                new SkillNode("mage.second-wind", "Second Wind",
                        "A practiced breath that bends time just enough to catch it.", 5, "mage.deep-freeze",
                        new AddAbility(new BasicAbility(
                                "mage.second-wind", "Second Wind",
                                "A brief personal fold in time, enough to recover.",
                                "Restores 40 stamina",
                                TargetType.SELF, 0,
                                AbilityEffect.restoreStamina(40)))),
                new SkillNode("mage.temporal-anchor", "Temporal Anchor",
                        "Bends time further, recovering even more.", 8, "mage.second-wind",
                        new UpgradeAbility(new BasicAbility(
                                "mage.second-wind", "Second Wind",
                                "A brief personal fold in time, enough to recover.",
                                "Restores 60 stamina",
                                TargetType.SELF, 0,
                                AbilityEffect.restoreStamina(60)))),
                new SkillNode("mage.chrono-shatter", "Chrono Shatter",
                        "Shatters a frozen moment directly into the enemy.", 12, "mage.temporal-anchor",
                        new AddAbility(new BasicAbility(
                                "mage.chrono-shatter", "Chrono Shatter",
                                "Collapses a held instant of time onto a single foe.",
                                "14 damage + Frozen for 2 turns",
                                TargetType.SINGLE_ENEMY, 60,
                                AbilityEffect.damageAndApplyEffect(14,
                                        actor -> ActiveEffect.frozen("Frozen", "frozen", 2))))),
                new SkillNode("mage.overclock", "Overclock",
                        "Time doesn't just favor the Mage anymore.", 12, "mage.temporal-anchor",
                        new AddAbility(new BasicAbility(
                                "mage.overclock", "Overclock",
                                "Shares a fold in time with the whole party.",
                                "Restores 25 stamina to all allies",
                                TargetType.ALL_ALLIES, 75,
                                AbilityEffect.restoreStamina(25))))
        ));
    }
}
