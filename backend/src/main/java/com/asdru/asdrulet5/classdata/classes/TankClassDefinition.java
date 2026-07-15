package com.asdru.asdrulet5.classdata.classes;

import com.asdru.asdrulet5.classdata.domain.*;
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
                new Stats(160, 18, 90),
                List.of(
                        new BasicAbility("tank.shield-bash", "Shield Bash",
                                "Slams an enemy with a shield, staggering them.",
                                "16 damage",
                                TargetType.SINGLE_ENEMY, 15,
                                AbilityEffect.damage(16)),
                        new BasicAbility("tank.taunt", "Taunt",
                                "Braces defensively, daring enemies to attack.",
                                "+10 defense for 2 turns",
                                TargetType.SELF, 10,
                                AbilityEffect.buffDefense("Taunt", "shield", 10, 2)),
                        new UltimateAbility("tank.fortress-stance", "Fortress Stance",
                                "Braces for impact, sharply raising defense.",
                                "+25 defense for 3 turns",
                                TargetType.SELF, 100,
                                AbilityEffect.buffDefense("Fortress Stance", "shield", 25, 3))
                )
        );
    }
}
