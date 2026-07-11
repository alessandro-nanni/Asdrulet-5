package com.asdru.asdrulet5.classdata.classes;

import com.asdru.asdrulet5.classdata.domain.BasicAbility;
import com.asdru.asdrulet5.classdata.domain.BuffDefenseEffect;
import com.asdru.asdrulet5.classdata.domain.ClassDefinition;
import com.asdru.asdrulet5.classdata.domain.DamageEffect;
import com.asdru.asdrulet5.classdata.domain.Stats;
import com.asdru.asdrulet5.classdata.domain.TargetType;
import com.asdru.asdrulet5.classdata.domain.UltimateAbility;
import com.asdru.asdrulet5.party.domain.CharacterClass;
import lombok.experimental.UtilityClass;

import java.util.List;

@UtilityClass
public class TankClassDefinition {

    public ClassDefinition define() {
        return new ClassDefinition(
                CharacterClass.TANK,
                "Tank",
                "A wall of steel that dares the enemy to try.",
                new Stats(160, 10, 18, 6, 90),
                List.of(
                        new BasicAbility("tank.shield-bash", "Shield Bash",
                                "Slams an enemy with a shield, staggering them.",
                                TargetType.SINGLE_ENEMY, 15,
                                new DamageEffect(16)),
                        new BasicAbility("tank.taunt", "Taunt",
                                "Braces defensively, daring enemies to attack.",
                                TargetType.SELF, 10,
                                new BuffDefenseEffect(10, 2)),
                        new UltimateAbility("tank.fortress-stance", "Fortress Stance",
                                "Braces for impact, sharply raising defense.",
                                TargetType.SELF, 100,
                                new BuffDefenseEffect(25, 3))
                )
        );
    }
}
