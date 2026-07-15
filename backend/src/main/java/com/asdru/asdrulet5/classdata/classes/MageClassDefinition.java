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
                new Stats(70, 2, 90),
                List.of(
                        new BasicAbility("mage.arcane-blast", "Arcane Blast",
                                "Detonates arcane energy across every enemy.",
                                "9 damage to all enemies",
                                TargetType.ALL_ENEMIES, 60,
                                AbilityEffect.damage(9)),
                        new BasicAbility("mage.frost-lock", "Frost Lock",
                                "Encases an enemy in ice, locking them in place.",
                                "Frozen for 2 turns",
                                TargetType.SINGLE_ENEMY, 80,
                                AbilityEffect.applyEffect(() -> ActiveEffect.frozen("Frozen", "frozen", 2))),
                        new UltimateAbility("mage.temporal-surge", "Temporal Surge",
                                "Bends time to refresh the party's energy while a bolt of raw force finds its mark.",
                                "+20 stamina to all allies, 20 damage",
                                TargetType.SINGLE_ENEMY, 100,
                                AbilityEffect.damageWithTeamStaminaBoost(20, 20))
                )
        );
    }
}
