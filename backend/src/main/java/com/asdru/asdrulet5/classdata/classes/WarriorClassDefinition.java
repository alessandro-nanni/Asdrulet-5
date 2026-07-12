package com.asdru.asdrulet5.classdata.classes;

import com.asdru.asdrulet5.classdata.domain.*;
import com.asdru.asdrulet5.party.domain.CharacterClass;
import lombok.experimental.UtilityClass;

import java.util.List;

@UtilityClass
public class WarriorClassDefinition {

    public ClassDefinition define() {
        return new ClassDefinition(
                CharacterClass.WARRIOR,
                "Warrior",
                "Charges in first and asks questions never.",
                new Stats(120, 18, 10, 100),
                List.of(
                        new BasicAbility("warrior.cleave", "Cleave",
                                "A wide sweeping strike that cuts through an enemy's guard.",
                                "22 damage",
                                TargetType.SINGLE_ENEMY, 15,
                                AbilityEffect.damage(22)),
                        new BasicAbility("warrior.rally", "Rally",
                                "A battle cry that steels the warrior's resolve.",
                                "+8 damage for 2 turns",
                                TargetType.SELF, 10,
                                AbilityEffect.buffDamage("Rally", "sword", 8, 2)),
                        new BasicAbility("warrior.blade-flurry", "Blade Flurry",
                                "Four rapid slices, each one finding a gap in the enemy's guard.",
                                "4 hits of 5 damage",
                                TargetType.SINGLE_ENEMY, 18,
                                AbilityEffect.multiHitDamage(4, 5)),
                        new UltimateAbility("warrior.reckless-onslaught", "Reckless Onslaught",
                                "A relentless flurry of blows against a single target.",
                                "45 damage",
                                TargetType.SINGLE_ENEMY, 100,
                                AbilityEffect.damage(45))
                )
        );
    }
}
