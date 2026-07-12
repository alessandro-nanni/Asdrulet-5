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
                                TargetType.SINGLE_ENEMY, 15,
                                new DamageEffect(22)),
                        new BasicAbility("warrior.rally", "Rally",
                                "A battle cry that steels the warrior's resolve.",
                                TargetType.SELF, 10,
                                new BuffDamageEffect(8, 2)),
                        new UltimateAbility("warrior.reckless-onslaught", "Reckless Onslaught",
                                "A relentless flurry of blows against a single target.",
                                TargetType.SINGLE_ENEMY, 100,
                                new DamageEffect(45))
                )
        );
    }
}
