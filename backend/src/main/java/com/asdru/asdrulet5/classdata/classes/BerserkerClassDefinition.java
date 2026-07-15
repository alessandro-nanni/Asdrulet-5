package com.asdru.asdrulet5.classdata.classes;

import com.asdru.asdrulet5.classdata.domain.*;
import com.asdru.asdrulet5.party.domain.CharacterClass;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class BerserkerClassDefinition {

    private static final double CRIT_CHANCE = 0.2;

    public ClassDefinition define(boolean includeTestAbility) {
        List<Ability> abilities = new ArrayList<>(List.of(
                new BasicAbility("berserker.reckless-strike", "Reckless Strike",
                        "A wild swing with a chance to land a brutal blow.",
                        "12 damage, 20% chance to double",
                        TargetType.SINGLE_ENEMY, 30,
                        AbilityEffect.critDamage(12, CRIT_CHANCE)),
                new BasicAbility("berserker.battle-fury", "Battle Fury",
                        "Rallies an ally into a battle-frenzy.",
                        "+5% damage for 3 turns",
                        TargetType.SINGLE_ALLY, 40,
                        AbilityEffect.applyEffect(() -> ActiveEffect.strength("Strength", "strength", 5, 3))),
                new UltimateAbility("berserker.bloodbath", "Bloodbath",
                        "A frenzied flurry of blows, each one a chance to cut deep.",
                        "4 hits of 9 damage, 20% chance to double each",
                        TargetType.SINGLE_ENEMY, 100,
                        AbilityEffect.multiHitCritDamage(4, 9, CRIT_CHANCE))
        ));
        if (includeTestAbility) {
            abilities.add(new BasicAbility("berserker.debug-nuke", "Debug Nuke",
                    "Dev tools only: instantly clears the room for testing.",
                    "1000 damage to all enemies",
                    TargetType.ALL_ENEMIES, 0,
                    AbilityEffect.damage(1000)));
        }
        return new ClassDefinition(
                CharacterClass.BERSERKER,
                "Berserker",
                "Charges in first and asks questions never.",
                new Stats(80, 1, 60),
                abilities
        );
    }
}
