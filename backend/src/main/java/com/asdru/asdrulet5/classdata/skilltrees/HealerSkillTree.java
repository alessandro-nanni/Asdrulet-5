package com.asdru.asdrulet5.classdata.skilltrees;

import com.asdru.asdrulet5.classdata.domain.*;
import com.asdru.asdrulet5.party.domain.CharacterClass;
import lombok.experimental.UtilityClass;

import java.util.List;

@UtilityClass
public class HealerSkillTree {

    public SkillTree define() {
        return new SkillTree(CharacterClass.HEALER, List.of(
                new SkillNode("healer.attunement", "Attunement",
                        "A deeper communion with the light — Healing Light mends more.", 2, null,
                        new UpgradeAbility(new BasicAbility(
                                "healer.healing-light", "Healing Light",
                                "Channels warm light into a single ally, mending their wounds.",
                                "20 healing",
                                TargetType.SINGLE_ALLY, 45,
                                AbilityEffect.heal(20)))),

                new SkillNode("healer.overflow", "Overflow",
                        "Circle of Renewal floods the party with even more light.", 3, "healer.attunement",
                        new UpgradeAbility(new UltimateAbility(
                                "healer.circle-of-renewal", "Circle of Renewal",
                                "Floods the party with cleansing light, purging harm and mending wounds.",
                                "Clears negative effects, 30 healing to all allies",
                                TargetType.ALL_ALLIES, 100,
                                AbilityEffect.healAndClearNegativeEffects(30)))),
                new SkillNode("healer.blessed-renewal", "Blessed Renewal",
                        "Leaves a lingering blessing that keeps mending an ally over time.", 5, "healer.overflow",
                        new AddAbility(new BasicAbility(
                                "healer.blessed-renewal", "Blessed Renewal",
                                "Wraps an ally in a lingering blessing.",
                                "8 healing per turn for 3 turns",
                                TargetType.SINGLE_ALLY, 35,
                                AbilityEffect.healOverTime("Blessed", "Mending steadily each turn.", "heal", 8, 3)))),
                new SkillNode("healer.deeper-blessing", "Deeper Blessing",
                        "The blessing lingers longer and mends deeper.", 8, "healer.blessed-renewal",
                        new UpgradeAbility(new BasicAbility(
                                "healer.blessed-renewal", "Blessed Renewal",
                                "Wraps an ally in a lingering blessing.",
                                "12 healing per turn for 4 turns",
                                TargetType.SINGLE_ALLY, 35,
                                AbilityEffect.healOverTime("Blessed", "Mending steadily each turn.", "heal", 12, 4)))),
                new SkillNode("healer.sanctuary", "Sanctuary",
                        "Extends the blessing to the whole party at once.", 12, "healer.deeper-blessing",
                        new AddAbility(new BasicAbility(
                                "healer.sanctuary", "Sanctuary",
                                "A hallowed ground that mends everyone standing on it.",
                                "6 healing per turn for 3 turns, to all allies",
                                TargetType.ALL_ALLIES, 60,
                                AbilityEffect.healOverTime("Sanctuary", "Mending steadily each turn.", "heal", 6, 3)))),
                new SkillNode("healer.mending-wave", "Mending Wave",
                        "A single wave of light instead of a lingering glow.", 12, "healer.deeper-blessing",
                        new AddAbility(new BasicAbility(
                                "healer.mending-wave", "Mending Wave",
                                "A wave of restorative light washing over the whole party.",
                                "15 healing to all allies",
                                TargetType.ALL_ALLIES, 55,
                                AbilityEffect.heal(15)))),

                new SkillNode("healer.radiant-wrath", "Radiant Wrath",
                        "Golden Strike burns brighter, and marks its target longer.", 3, "healer.attunement",
                        new UpgradeAbility(new BasicAbility(
                                "healer.golden-strike", "Golden Strike",
                                "Smites a foe with radiant force, marking them for allies to profit from.",
                                "14 damage + Golden Touch for 3 turns",
                                TargetType.SINGLE_ENEMY, 50,
                                AbilityEffect.damageAndApplyEffect(14,
                                        actor -> ActiveEffect.goldenTouch("Golden Touch", "goldenTouch", 3))))),
                new SkillNode("healer.smite", "Smite",
                        "Channels the light into an outright weapon.", 5, "healer.radiant-wrath",
                        new AddAbility(new BasicAbility(
                                "healer.smite", "Smite",
                                "A concentrated bolt of pure, punishing light.",
                                "18 damage, 25% chance to double",
                                TargetType.SINGLE_ENEMY, 40,
                                AbilityEffect.critDamage(18, 0.25)))),
                new SkillNode("healer.judgment", "Judgment",
                        "The light no longer just wounds — it judges.", 8, "healer.smite",
                        new UpgradeAbility(new BasicAbility(
                                "healer.smite", "Smite",
                                "A concentrated bolt of pure, punishing light.",
                                "24 damage, 30% chance to double",
                                TargetType.SINGLE_ENEMY, 40,
                                AbilityEffect.critDamage(24, 0.3)))),
                new SkillNode("healer.consecrated-ground", "Consecrated Ground",
                        "Judgment spreads to everything nearby.", 12, "healer.judgment",
                        new AddAbility(new BasicAbility(
                                "healer.consecrated-ground", "Consecrated Ground",
                                "Hallowed light scours every foe at once.",
                                "9 damage to all enemies",
                                TargetType.ALL_ENEMIES, 65,
                                AbilityEffect.damage(9)))),
                new SkillNode("healer.executioners-light", "Executioner's Light",
                        "One final, blinding verdict.", 12, "healer.judgment",
                        new AddAbility(new BasicAbility(
                                "healer.executioners-light", "Executioner's Light",
                                "A blinding verdict left for only the most stubborn foe.",
                                "28 damage, 35% chance to double",
                                TargetType.SINGLE_ENEMY, 50,
                                AbilityEffect.critDamage(28, 0.35))))
        ));
    }
}
