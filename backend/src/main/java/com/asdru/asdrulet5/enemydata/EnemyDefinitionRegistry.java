package com.asdru.asdrulet5.enemydata;

import com.asdru.asdrulet5.classdata.domain.AbilityEffect;
import com.asdru.asdrulet5.classdata.domain.BasicAbility;
import com.asdru.asdrulet5.classdata.domain.Stats;
import com.asdru.asdrulet5.classdata.domain.TargetType;
import com.asdru.asdrulet5.enemydata.domain.EnemyDefinition;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Static catalog of enemy definitions. To add a new enemy: add a factory
 * method here and add it to {@link #buildDefinitions()} — then give it an
 * entry in {@code EnemyEncounterRegistry} to actually make it spawnable
 * anywhere (this registry just describes what an enemy is, the same way
 * {@code ItemDefinitionRegistry} does for items).
 *
 * <p>Every enemy below declares more than one {@link BasicAbility}, ordered
 * strongest/most expensive first and a cheap (typically 0-stamina-cost)
 * fallback attack last — see {@code Combat.chooseEnemyAbility}, which always
 * uses the first one an enemy can currently afford. Enemies regenerate
 * stamina on their own turn exactly like players do ({@code
 * Combat.STAMINA_REGEN_PER_TURN}), so a stronger attack's cost is
 * deliberately set above that per-turn regen (rather than merely at or below
 * {@code maxStamina}) — otherwise it would simply be affordable, and so
 * chosen, every single turn. Sized this way, an enemy naturally alternates
 * between banking stamina on its cheap fallback and unleashing its stronger
 * attack once it's saved enough, instead of ever settling on one move for
 * the whole fight.
 *
 * <p>{@link #DEFAULT_ENEMY_ID} (Goblin Marauder) is reserved for boss
 * encounters — see {@code EnemyEncounterRegistry}'s own doc — the other
 * enemies here are the regular floor-1 pool, individually weaker since 2 or
 * more of them show up together instead of one at a time. The boss's own
 * three-tier move set is deliberately deeper than a regular enemy's two, to
 * feel like a distinct, tougher fight rather than just "a bigger regular
 * enemy."
 */
@Component
public class EnemyDefinitionRegistry {

    public static final String DEFAULT_ENEMY_ID = "goblin-marauder";

    private static final Map<String, EnemyDefinition> DEFINITIONS = buildDefinitions();

    private static Map<String, EnemyDefinition> buildDefinitions() {
        return Stream.of(goblinMarauder(), caveRat(), goblinSkirmisher(), banditThug())
                .collect(Collectors.toMap(EnemyDefinition::id, Function.identity()));
    }

    private static EnemyDefinition goblinMarauder() {
        // 90 maxStamina: exactly affords Warlord's Fury from a full-stamina
        // opening turn, then (see the class doc) can't afford it again until
        // it's banked 90 back up — Brutal Swing's own 45 cost is exactly one
        // turn's worth of regen above STAMINA_REGEN_PER_TURN (40), so it's
        // the steady in-between attack rather than an immediate repeat.
        BasicAbility warlordsFury = new BasicAbility(
                "goblin-marauder.warlords-fury", "Warlord's Fury",
                "Channels every last drop of rage into one devastating blow.", "30 damage",
                TargetType.SINGLE_ENEMY, 90, AbilityEffect.damage(30));
        BasicAbility brutalSwing = new BasicAbility(
                "goblin-marauder.brutal-swing", "Brutal Swing",
                "A heavier follow-up, swung with both hands.", "20 damage",
                TargetType.SINGLE_ENEMY, 45, AbilityEffect.damage(20));
        BasicAbility rustyCleaver = new BasicAbility(
                "goblin-marauder.rusty-cleaver", "Rusty Cleaver",
                "Swings a notched blade at whoever's closest.", "15 damage",
                TargetType.SINGLE_ENEMY, 0, AbilityEffect.damage(15));
        return new EnemyDefinition(
                DEFAULT_ENEMY_ID,
                "Goblin Marauder",
                new Stats(220, 8, 90),
                List.of(warlordsFury, brutalSwing, rustyCleaver),
                List.of()
        );
    }

    private static EnemyDefinition caveRat() {
        // 50 cost/maxStamina, above the 40-per-turn regen: opens with
        // Frenzied Bite, can't afford it again next turn (40 < 50), falls
        // back to the free Gnawing Bite, then affords Frenzied Bite again
        // the turn after — a clean bite/bite-harder/bite alternation.
        BasicAbility frenziedBite = new BasicAbility(
                "cave-rat.frenzied-bite", "Frenzied Bite",
                "A rabid, all-or-nothing lunge.", "14 damage",
                TargetType.SINGLE_ENEMY, 50, AbilityEffect.damage(14));
        BasicAbility gnawingBite = new BasicAbility(
                "cave-rat.gnawing-bite", "Gnawing Bite",
                "Latches on and gnaws with needle-sharp teeth.", "8 damage",
                TargetType.SINGLE_ENEMY, 0, AbilityEffect.damage(8));
        return new EnemyDefinition(
                "cave-rat",
                "Cave Rat",
                new Stats(60, 1, 50),
                List.of(frenziedBite, gnawingBite),
                List.of()
        );
    }

    private static EnemyDefinition goblinSkirmisher() {
        BasicAbility doubleSlash = new BasicAbility(
                "goblin-skirmisher.double-slash", "Double Slash",
                "Two quick cuts faster than the eye can follow.", "16 damage",
                TargetType.SINGLE_ENEMY, 55, AbilityEffect.damage(16));
        BasicAbility jaggedDagger = new BasicAbility(
                "goblin-skirmisher.jagged-dagger", "Jagged Dagger",
                "Darts in for a quick stab, then dances back.", "10 damage",
                TargetType.SINGLE_ENEMY, 0, AbilityEffect.damage(10));
        return new EnemyDefinition(
                "goblin-skirmisher",
                "Goblin Skirmisher",
                new Stats(90, 4, 55),
                List.of(doubleSlash, jaggedDagger),
                List.of()
        );
    }

    private static EnemyDefinition banditThug() {
        BasicAbility haymaker = new BasicAbility(
                "bandit-thug.haymaker", "Haymaker",
                "Winds up and throws every ounce of weight behind one punch.", "20 damage",
                TargetType.SINGLE_ENEMY, 60, AbilityEffect.damage(20));
        BasicAbility clubSwing = new BasicAbility(
                "bandit-thug.club-swing", "Club Swing",
                "A clumsy but heavy overhead swing.", "12 damage",
                TargetType.SINGLE_ENEMY, 0, AbilityEffect.damage(12));
        return new EnemyDefinition(
                "bandit-thug",
                "Bandit Thug",
                new Stats(110, 6, 60),
                List.of(haymaker, clubSwing),
                List.of()
        );
    }

    public List<EnemyDefinition> all() {
        return List.copyOf(DEFINITIONS.values());
    }

    public EnemyDefinition get(String id) {
        EnemyDefinition definition = DEFINITIONS.get(id);
        if (definition == null) {
            throw new IllegalArgumentException("No enemy definition registered for " + id);
        }
        return definition;
    }
}
