package com.asdru.asdrulet5.classdata.classes;

import com.asdru.asdrulet5.classdata.domain.*;
import com.asdru.asdrulet5.party.domain.CharacterClass;
import lombok.experimental.UtilityClass;

import java.util.List;

@UtilityClass
public class PaladinClassDefinition {

    public ClassDefinition define() {
        return new ClassDefinition(
                CharacterClass.PALADIN,
                "Paladin",
                "A wall of steel that dares the enemy to try.",
                new Stats(90, 3, 50),
                List.of(
                        new BasicAbility("paladin.shield-slam", "Shield Slam",
                                "Slams a foe with a shield, daring them to strike back.",
                                "4 damage + Taunt for 2 turns",
                                TargetType.SINGLE_ENEMY, 25,
                                AbilityEffect.damageAndApplyEffect(4,
                                        actor -> ActiveEffect.taunt("Taunt", "taunt", 2, actor.combatantId(), actor.displayName()))),
                        new BasicAbility("paladin.protect", "Protect",
                                "Raises a defensive stance, bracing for impact.",
                                "+12 defense for 2 turns",
                                TargetType.SELF, 40,
                                AbilityEffect.buffDefense("Protection", "shield", 12, 2)),
                        new UltimateAbility("paladin.bulwark", "Bulwark",
                                "Draws every enemy's ire and answers with retribution.",
                                "Taunts all enemies for 1 turn, gains Thorns for 2 turns",
                                TargetType.ALL_ENEMIES, 100,
                                AbilityEffect.tauntAndSelfThorns(1, 2))
                )
        );
    }
}
