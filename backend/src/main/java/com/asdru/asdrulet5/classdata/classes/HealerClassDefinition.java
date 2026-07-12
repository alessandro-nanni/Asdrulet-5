package com.asdru.asdrulet5.classdata.classes;

import com.asdru.asdrulet5.classdata.domain.*;
import com.asdru.asdrulet5.party.domain.CharacterClass;
import lombok.experimental.UtilityClass;

import java.util.List;

@UtilityClass
public class HealerClassDefinition {

    public ClassDefinition define() {
        return new ClassDefinition(
                CharacterClass.HEALER,
                "Healer",
                "A steady hand that keeps the party standing when everything else falls apart.",
                new Stats(80, 8, 4, 100),
                List.of(
                        new BasicAbility("healer.mending-light", "Mending Light",
                                "Channels restorative light into a single ally.",
                                "20 healing",
                                TargetType.SINGLE_ALLY, 15,
                                AbilityEffect.heal(20)),
                        new BasicAbility("healer.smite", "Smite",
                                "Calls down a bolt of holy energy on an enemy.",
                                "18 damage",
                                TargetType.SINGLE_ENEMY, 10,
                                AbilityEffect.damage(18)),
                        new UltimateAbility("healer.circle-of-renewal", "Circle of Renewal",
                                "Floods the whole party with healing light.",
                                "35 healing to all allies",
                                TargetType.ALL_ALLIES, 100,
                                AbilityEffect.heal(35))
                )
        );
    }
}
