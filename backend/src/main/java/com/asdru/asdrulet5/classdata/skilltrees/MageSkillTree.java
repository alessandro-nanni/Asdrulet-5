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
                                AbilityEffect.restoreStamina(40))))
        ));
    }
}
