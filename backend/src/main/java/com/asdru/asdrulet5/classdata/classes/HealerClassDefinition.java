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
                new Stats(60, 0, 80),
                List.of(
                        new BasicAbility("healer.healing-light", "Healing Light",
                                "Channels warm light into a single ally, mending their wounds.",
                                "15 healing",
                                TargetType.SINGLE_ALLY, 45,
                                AbilityEffect.heal(15)),
                        new BasicAbility("healer.golden-strike", "Golden Strike",
                                "Smites a foe with radiant force, marking them for allies to profit from.",
                                "8 damage + Golden Touch for 2 turns",
                                TargetType.SINGLE_ENEMY, 50,
                                AbilityEffect.damageAndApplyEffect(8,
                                        actor -> ActiveEffect.goldenTouch("Golden Touch", "goldenTouch", 2))),
                        new UltimateAbility("healer.circle-of-renewal", "Circle of Renewal",
                                "Floods the party with cleansing light, purging harm and mending wounds.",
                                "Clears negative effects, 20 healing to all allies",
                                TargetType.ALL_ALLIES, 100,
                                AbilityEffect.healAndClearNegativeEffects(20))
                )
        );
    }
}
