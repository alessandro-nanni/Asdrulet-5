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
                                TargetType.SINGLE_ALLY, 15,
                                new HealEffect(20)),
                        new BasicAbility("healer.smite", "Smite",
                                "Calls down a bolt of holy energy on an enemy.",
                                TargetType.SINGLE_ENEMY, 10,
                                new DamageEffect(18)),
                        new UltimateAbility("healer.circle-of-renewal", "Circle of Renewal",
                                "Floods the whole party with healing light.",
                                TargetType.ALL_ALLIES, 100,
                                new HealEffect(35))
                )
        );
    }
}
