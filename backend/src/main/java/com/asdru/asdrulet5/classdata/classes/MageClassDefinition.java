package com.asdru.asdrulet5.classdata.classes;

import com.asdru.asdrulet5.classdata.domain.*;
import com.asdru.asdrulet5.party.domain.CharacterClass;
import lombok.experimental.UtilityClass;

import java.util.List;

@UtilityClass
public class MageClassDefinition {

    public ClassDefinition define() {
        return new ClassDefinition(
                CharacterClass.MAGE,
                "Mage",
                "Fragile, but the last thing a lot of enemies ever see.",
                new Stats(70, 22, 3, 8, 110),
                List.of(
                        new BasicAbility("mage.firebolt", "Firebolt",
                                "Launches a bolt of searing flame at an enemy.",
                                TargetType.SINGLE_ENEMY, 12,
                                new DamageEffect(24)),
                        new BasicAbility("mage.frost-lance", "Frost Lance",
                                "Impales an enemy with ice, slowing them.",
                                TargetType.SINGLE_ENEMY, 14,
                                new DamageEffect(20)),
                        new UltimateAbility("mage.meteor-storm", "Meteor Storm",
                                "Calls a hail of meteors down on every enemy.",
                                TargetType.ALL_ENEMIES, 100,
                                new DamageEffect(30))
                )
        );
    }
}
