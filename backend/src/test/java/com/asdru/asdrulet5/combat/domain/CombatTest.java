package com.asdru.asdrulet5.combat.domain;

import com.asdru.asdrulet5.classdata.domain.*;
import com.asdru.asdrulet5.combat.exception.CombatNotInProgressException;
import com.asdru.asdrulet5.combat.exception.InsufficientResourceException;
import com.asdru.asdrulet5.combat.exception.NotYourTurnException;
import com.asdru.asdrulet5.inventory.domain.ItemPassive;
import com.asdru.asdrulet5.party.domain.CharacterClass;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CombatTest {

    private static final BasicAbility STRIKE = new BasicAbility(
            "test.strike", "Strike", "A basic attack.", "20 damage", TargetType.SINGLE_ENEMY, 30,
            AbilityEffect.damage(20));

    private static final BasicAbility EXPENSIVE_STRIKE = new BasicAbility(
            "test.expensive-strike", "Expensive Strike", "A costly attack.", "20 damage", TargetType.SINGLE_ENEMY, 90,
            AbilityEffect.damage(20));

    private static final BasicAbility MEND = new BasicAbility(
            "test.mend", "Mend", "A basic heal.", "15 healing", TargetType.SINGLE_ALLY, 10,
            AbilityEffect.heal(15));

    private static final BasicAbility BRACE = new BasicAbility(
            "test.brace", "Brace", "Raises defense.", "+10 defense for 1 turn", TargetType.SELF, 10,
            AbilityEffect.buffDefense("Brace", "shield", 10, 1));

    private static final BasicAbility PUMP_UP = new BasicAbility(
            "test.pump-up", "Pump Up", "Raises damage.", "+15 damage for 2 turns", TargetType.SELF, 10,
            AbilityEffect.buffDamage("Pump Up", "sword", 15, 2));

    private static final UltimateAbility ULTIMATE_STRIKE = new UltimateAbility(
            "test.ultimate-strike", "Ultimate Strike", "A big attack.", "50 damage", TargetType.SINGLE_ENEMY, 40,
            AbilityEffect.damage(50));

    private static final BasicAbility MULTI_STRIKE = new BasicAbility(
            "test.multi-strike", "Multi Strike", "Hits four times in rapid succession.", "4 hits of 5 damage",
            TargetType.SINGLE_ENEMY, 10,
            AbilityEffect.multiHitDamage(4, 5));

    private static final BasicAbility POISON_DART = new BasicAbility(
            "test.poison-dart", "Poison Dart", "Applies a lingering poison.", "5 poison damage per turn for 2 turns",
            TargetType.SINGLE_ENEMY, 10,
            AbilityEffect.damageOverTime("Poisoned", "Takes damage each turn.", "poison", 5, 2));

    private static Combatant player(String id, List<Ability> abilities) {
        return player(id, abilities, List.of());
    }

    private static Combatant player(String id, List<Ability> abilities, List<ItemPassive> passives) {
        return new Combatant(id, id, false, CharacterClass.WARRIOR, 100, 100, 5, 0, 40, abilities, null, null, null, null, passives);
    }

    private static Combatant enemy(String id, int maxHealth, int defense, int attackPower) {
        return enemy(id, maxHealth, defense, attackPower, List.of());
    }

    private static Combatant enemy(String id, int maxHealth, int defense, int attackPower, List<ItemPassive> passives) {
        return new Combatant(id, id, true, null, maxHealth, 0, defense, 0, 0, List.of(),
                "Claw", "A swipe.", attackPower + " damage", AbilityEffect.damage(attackPower), passives);
    }

    private static Combat twoPlayersOneEnemy(Combatant p1, Combatant p2, Combatant enemy) {
        return new Combat("ABC123", List.of(p1, p2, enemy), List.of(p1.id(), p2.id(), enemy.id()));
    }

    private static Combatant findCombatant(Combat combat, String id) {
        return combat.combatants().stream().filter(c -> c.id().equals(id)).findFirst().orElseThrow();
    }

    @Test
    void usingBasicAbilityDealsDamageReducedByDefenseAndSpendsStamina() {
        Combatant p1 = player("p1", List.of(STRIKE, MEND, ULTIMATE_STRIKE));
        Combatant p2 = player("p2", List.of(STRIKE, MEND, ULTIMATE_STRIKE));
        Combatant enemy = enemy("enemy", 200, 6, 10);
        Combat combat = twoPlayersOneEnemy(p1, p2, enemy);

        combat.useAbility("p1", "test.strike", "enemy");

        Combatant updatedEnemy = findCombatant(combat, "enemy");
        Combatant updatedP1 = findCombatant(combat, "p1");
        // Mitigation = 6 / (6 + 25) defense-half-point; 20 * (1 - 6/31) rounds to 16.
        assertThat(updatedEnemy.currentHealth()).isEqualTo(200 - 16);
        assertThat(updatedP1.currentStamina()).isEqualTo(100 - 30);
    }

    @Test
    void damageIsClampedToAtLeastOne() {
        Combatant p1 = player("p1", List.of(STRIKE, MEND, ULTIMATE_STRIKE));
        Combatant p2 = player("p2", List.of(STRIKE, MEND, ULTIMATE_STRIKE));
        Combatant enemy = enemy("enemy", 200, 999, 10);
        Combat combat = twoPlayersOneEnemy(p1, p2, enemy);

        combat.useAbility("p1", "test.strike", "enemy");

        // Mitigation approaches, but never reaches, 100%; the 1-damage floor still applies.
        assertThat(findCombatant(combat, "enemy").currentHealth()).isEqualTo(199);
    }

    @Test
    void usingAbilityWithoutEnoughStaminaThrows() {
        Combatant p1 = player("p1", List.of(EXPENSIVE_STRIKE));
        Combatant p2 = player("p2", List.of(STRIKE));
        Combatant enemy = enemy("enemy", 200, 5, 10);
        Combat combat = twoPlayersOneEnemy(p1, p2, enemy);

        combat.useAbility("p1", "test.expensive-strike", "enemy");

        assertThatThrownBy(() -> combat.useAbility("p1", "test.expensive-strike", "enemy"))
                .isInstanceOf(InsufficientResourceException.class);
    }

    @Test
    void usingUltimateBeforeChargedThrows() {
        Combatant p1 = player("p1", List.of(STRIKE, ULTIMATE_STRIKE));
        Combatant p2 = player("p2", List.of(STRIKE));
        Combatant enemy = enemy("enemy", 200, 5, 10);
        Combat combat = twoPlayersOneEnemy(p1, p2, enemy);

        assertThatThrownBy(() -> combat.useAbility("p1", "test.ultimate-strike", "enemy"))
                .isInstanceOf(InsufficientResourceException.class);
    }

    @Test
    void ultimateChargesFromDamageDealtAndResetsAfterUse() {
        Combatant p1 = player("p1", List.of(STRIKE, ULTIMATE_STRIKE));
        Combatant p2 = player("p2", List.of(STRIKE));
        Combatant enemy = enemy("enemy", 200, 5, 10);
        Combat combat = twoPlayersOneEnemy(p1, p2, enemy);

        // Each strike deals round(20 * 25/30) = 17 damage against 5 defense; charge threshold is 40.
        combat.useAbility("p1", "test.strike", "enemy");
        assertThatThrownBy(() -> combat.useAbility("p1", "test.ultimate-strike", "enemy"))
                .isInstanceOf(InsufficientResourceException.class);

        combat.useAbility("p1", "test.strike", "enemy");
        assertThat(findCombatant(combat, "p1").ultimateCharge()).isEqualTo(34);

        combat.useAbility("p1", "test.strike", "enemy");
        assertThat(findCombatant(combat, "p1").ultimateCharge()).isEqualTo(40);

        combat.useAbility("p1", "test.ultimate-strike", "enemy");
        // The ultimate's own damage (round(50 * 25/30) = 42) immediately re-adds charge, capped at threshold.
        assertThat(findCombatant(combat, "p1").ultimateCharge()).isEqualTo(40);
    }

    @Test
    void actingOutOfTurnThrows() {
        Combatant p1 = player("p1", List.of(STRIKE));
        Combatant p2 = player("p2", List.of(STRIKE));
        Combatant enemy = enemy("enemy", 200, 5, 10);
        Combat combat = twoPlayersOneEnemy(p1, p2, enemy);

        assertThatThrownBy(() -> combat.useAbility("p2", "test.strike", "enemy"))
                .isInstanceOf(NotYourTurnException.class);
    }

    @Test
    void endTurnAdvancesAndEnemyTurnResolvesAutomatically() {
        Combatant p1 = player("p1", List.of(STRIKE));
        Combatant p2 = player("p2", List.of(STRIKE));
        Combatant enemy = enemy("enemy", 200, 5, 12);
        Combat combat = twoPlayersOneEnemy(p1, p2, enemy);

        assertThat(combat.currentTurnCombatantId()).isEqualTo("p1");
        combat.endTurn("p1");
        assertThat(combat.currentTurnCombatantId()).isEqualTo("p2");

        combat.endTurn("p2");
        // Enemy's turn resolved automatically, cursor lands back on p1.
        assertThat(combat.currentTurnCombatantId()).isEqualTo("p1");
        Combatant lowestHealthPlayer = findCombatant(combat, "p1").currentHealth() <= findCombatant(combat, "p2").currentHealth()
                ? findCombatant(combat, "p1")
                : findCombatant(combat, "p2");
        // round(12 * 25/30) = 10 damage against 5 defense.
        assertThat(lowestHealthPlayer.currentHealth()).isEqualTo(100 - 10);
    }

    @Test
    void staminaPartiallyRegeneratesOnOwnNextTurnNotFullRefill() {
        Combatant p1 = player("p1", List.of(EXPENSIVE_STRIKE));
        Combatant p2 = player("p2", List.of(STRIKE));
        Combatant enemy = enemy("enemy", 200, 5, 1);
        Combat combat = twoPlayersOneEnemy(p1, p2, enemy);

        combat.useAbility("p1", "test.expensive-strike", "enemy");
        assertThat(findCombatant(combat, "p1").currentStamina()).isEqualTo(10);

        combat.endTurn("p1");
        combat.endTurn("p2");

        assertThat(combat.currentTurnCombatantId()).isEqualTo("p1");
        assertThat(findCombatant(combat, "p1").currentStamina()).isEqualTo(10 + Combat.STAMINA_REGEN_PER_TURN);
    }

    @Test
    void healRestoresHealthUpToMaxWithoutOverhealing() {
        Combatant p1 = player("p1", List.of(STRIKE, MEND));
        Combatant p2 = player("p2", List.of(MEND));
        Combatant enemy = enemy("enemy", 200, 5, 200);
        Combat combat = twoPlayersOneEnemy(p1, p2, enemy);

        combat.endTurn("p1");
        // Enemy hasn't acted yet; p2 heals themselves at full health, should stay capped at max.
        combat.useAbility("p2", "test.mend", "p2");
        assertThat(findCombatant(combat, "p2").currentHealth()).isEqualTo(100);
    }

    @Test
    void buffDefenseReducesIncomingDamageThenExpires() {
        Combatant p1 = player("p1", List.of(BRACE, STRIKE));
        Combatant p2 = player("p2", List.of(STRIKE));
        Combatant enemy = enemy("enemy", 200, 5, 20);
        Combat combat = twoPlayersOneEnemy(p1, p2, enemy);

        combat.useAbility("p1", "test.brace", "p1");
        combat.endTurn("p1");
        combat.endTurn("p2");
        // Enemy attacked p1 (lowest HP tiebreak) while Brace (defense +10, 1 turn) was active:
        // defense 15, round(20 * 25/40) = 13 dmg.
        assertThat(findCombatant(combat, "p1").currentHealth()).isEqualTo(100 - 13);

        // Brace had 1 turn remaining; it should have expired when p1's turn started again.
        combat.endTurn("p1");
        combat.endTurn("p2");
        // Second enemy attack with no buff active: defense 5, round(20 * 25/30) = 17 dmg,
        // on whichever player has lowest HP (p1, at 87).
        assertThat(findCombatant(combat, "p1").currentHealth()).isEqualTo(100 - 13 - 17);
    }

    @Test
    void buffDamageIncreasesOutgoingDamage() {
        Combatant p1 = player("p1", List.of(PUMP_UP, STRIKE));
        Combatant p2 = player("p2", List.of(STRIKE));
        Combatant enemy = enemy("enemy", 200, 5, 1);
        Combat combat = twoPlayersOneEnemy(p1, p2, enemy);

        combat.useAbility("p1", "test.pump-up", "p1");
        combat.useAbility("p1", "test.strike", "enemy");

        // (20 strike power + 15 buff) against 5 defense: round(35 * 25/30) = 29.
        assertThat(findCombatant(combat, "enemy").currentHealth()).isEqualTo(200 - 29);
    }

    @Test
    void reapplyingSameNamedBuffRefreshesDurationInsteadOfStacking() {
        Combatant p1 = player("p1", List.of(PUMP_UP));
        Combatant p2 = player("p2", List.of(STRIKE));
        Combatant enemy = enemy("enemy", 200, 5, 1);
        Combat combat = twoPlayersOneEnemy(p1, p2, enemy);

        combat.useAbility("p1", "test.pump-up", "p1");
        assertThat(findCombatant(combat, "p1").activeEffects()).hasSize(1);
        assertThat(findCombatant(combat, "p1").bonusDamage()).isEqualTo(15);

        // One full round ticks p1's own effect down from 2 turns to 1.
        combat.endTurn("p1");
        combat.endTurn("p2");
        assertThat(findCombatant(combat, "p1").activeEffects().get(0).remainingTurns()).isEqualTo(1);

        // Reapplying "Pump Up" refreshes the existing effect rather than adding a second one.
        combat.useAbility("p1", "test.pump-up", "p1");
        assertThat(findCombatant(combat, "p1").activeEffects()).hasSize(1);
        assertThat(findCombatant(combat, "p1").activeEffects().get(0).remainingTurns()).isEqualTo(2);
        assertThat(findCombatant(combat, "p1").bonusDamage()).isEqualTo(15);
    }

    @Test
    void distinctlyNamedBuffsOnTheSameStatCoexist() {
        BasicAbility alphaGuard = new BasicAbility(
                "test.alpha-guard", "Alpha Guard", "Raises defense.", "+10 defense for 2 turns",
                TargetType.SELF, 10, AbilityEffect.buffDefense("Alpha Guard", "shield", 10, 2));
        BasicAbility betaGuard = new BasicAbility(
                "test.beta-guard", "Beta Guard", "Raises defense further.", "+5 defense for 2 turns",
                TargetType.SELF, 10, AbilityEffect.buffDefense("Beta Guard", "shield", 5, 2));
        Combatant p1 = player("p1", List.of(alphaGuard, betaGuard));
        Combatant p2 = player("p2", List.of(STRIKE));
        Combatant enemy = enemy("enemy", 200, 5, 1);
        Combat combat = twoPlayersOneEnemy(p1, p2, enemy);

        combat.useAbility("p1", "test.alpha-guard", "p1");
        combat.useAbility("p1", "test.beta-guard", "p1");

        // Different names, both from base defense 5: 5 + 10 + 5 = 20.
        assertThat(findCombatant(combat, "p1").activeEffects()).hasSize(2);
        assertThat(findCombatant(combat, "p1").effectiveDefense()).isEqualTo(20);
    }

    @Test
    void multiHitDamageAppliesEachHitIndependentlyAndAccumulatesUltimateCharge() {
        Combatant p1 = player("p1", List.of(MULTI_STRIKE));
        Combatant p2 = player("p2", List.of(STRIKE));
        Combatant enemy = enemy("enemy", 200, 2, 1);
        Combat combat = twoPlayersOneEnemy(p1, p2, enemy);

        combat.useAbility("p1", "test.multi-strike", "enemy");

        // Each hit: round(5 * 25/27) = 5 damage against 2 defense; four hits = 20 total, each contributing to charge.
        assertThat(findCombatant(combat, "enemy").currentHealth()).isEqualTo(200 - 20);
        assertThat(findCombatant(combat, "p1").ultimateCharge()).isEqualTo(20);
    }

    @Test
    void multiHitAbilityRecordsOneEventPerHitNotOneCombinedEvent() {
        Combatant p1 = player("p1", List.of(MULTI_STRIKE));
        Combatant p2 = player("p2", List.of(STRIKE));
        Combatant enemy = enemy("enemy", 200, 2, 1);
        Combat combat = twoPlayersOneEnemy(p1, p2, enemy);

        combat.useAbility("p1", "test.multi-strike", "enemy");

        List<CombatEvent> events = combat.lastEvents();
        assertThat(events).hasSize(4);
        assertThat(events).allSatisfy(event -> {
            assertThat(event.targetId()).isEqualTo("enemy");
            assertThat(event.kind()).isEqualTo(CombatEvent.Kind.DAMAGE);
            assertThat(event.amount()).isEqualTo(5);
        });
    }

    @Test
    void lastEventsReplacedEachCallIncludingEnemyAutoAttack() {
        Combatant p1 = player("p1", List.of(STRIKE));
        Combatant p2 = player("p2", List.of(STRIKE));
        Combatant enemy = enemy("enemy", 200, 5, 12);
        Combat combat = twoPlayersOneEnemy(p1, p2, enemy);

        combat.useAbility("p1", "test.strike", "enemy");
        assertThat(combat.lastEvents()).hasSize(1);
        assertThat(combat.lastEvents().get(0).targetId()).isEqualTo("enemy");

        combat.endTurn("p1");
        // Nothing happened advancing to p2's turn: no enemy action yet, no active effects to tick.
        assertThat(combat.lastEvents()).isEmpty();

        combat.endTurn("p2");
        // Enemy's turn resolves automatically within this call; the p1-vs-enemy event from
        // the earlier useAbility call is gone, replaced by just the enemy's attack.
        List<CombatEvent> events = combat.lastEvents();
        assertThat(events).hasSize(1);
        CombatEvent event = events.get(0);
        assertThat(event.kind()).isEqualTo(CombatEvent.Kind.DAMAGE);
        // round(12 * 25/30) = 10 damage against 5 defense.
        assertThat(event.amount()).isEqualTo(10);
    }

    @Test
    void damageOverTimeTicksOnHolderTurnStartAndExpiresAfterDuration() {
        Combatant p1 = player("p1", List.of(POISON_DART));
        Combatant p2 = player("p2", List.of(STRIKE));
        Combatant enemy = enemy("enemy", 200, 5, 1);
        Combat combat = twoPlayersOneEnemy(p1, p2, enemy);

        combat.useAbility("p1", "test.poison-dart", "enemy");
        // Attaching the effect doesn't itself deal damage — onTick hasn't run yet.
        assertThat(findCombatant(combat, "enemy").currentHealth()).isEqualTo(200);
        assertThat(findCombatant(combat, "enemy").activeEffects()).hasSize(1);

        combat.endTurn("p1");
        combat.endTurn("p2");
        // Effects tick at the start of their holder's own turn; enemy's turn just started.
        assertThat(findCombatant(combat, "enemy").currentHealth()).isEqualTo(195);
        assertThat(findCombatant(combat, "enemy").activeEffects()).hasSize(1);

        combat.endTurn("p1");
        combat.endTurn("p2");
        // Second and final tick; the effect expires and is removed afterward.
        assertThat(findCombatant(combat, "enemy").currentHealth()).isEqualTo(190);
        assertThat(findCombatant(combat, "enemy").activeEffects()).isEmpty();

        combat.endTurn("p1");
        combat.endTurn("p2");
        // No longer active: no further ticks.
        assertThat(findCombatant(combat, "enemy").currentHealth()).isEqualTo(190);
    }

    @Test
    void combatEndsInPartyWonWhenAllEnemiesDefeated() {
        Combatant p1 = player("p1", List.of(STRIKE));
        Combatant p2 = player("p2", List.of(STRIKE));
        Combatant enemy = enemy("enemy", 15, 0, 1);
        Combat combat = twoPlayersOneEnemy(p1, p2, enemy);

        combat.useAbility("p1", "test.strike", "enemy");

        assertThat(combat.status()).isEqualTo(CombatStatus.PARTY_WON);
        assertThatThrownBy(() -> combat.endTurn("p2"))
                .isInstanceOf(CombatNotInProgressException.class);
    }

    @Test
    void combatEndsInPartyLostWhenAllPlayersDefeated() {
        Combatant p1 = player("p1", List.of(STRIKE));
        Combatant enemy = enemy("enemy", 200, 0, 200);
        Combat combat = new Combat("ABC123", List.of(p1, enemy), List.of("p1", "enemy"));

        combat.endTurn("p1");

        assertThat(combat.status()).isEqualTo(CombatStatus.PARTY_LOST);
    }

    @Test
    void allEnemiesTargetTypeHitsEveryAliveEnemy() {
        BasicAbility cleave = new BasicAbility(
                "test.cleave", "Cleave", "Hits every enemy.", "20 damage", TargetType.ALL_ENEMIES, 10,
                AbilityEffect.damage(20));
        Combatant p1 = player("p1", List.of(cleave));
        Combatant enemyA = enemy("enemyA", 200, 5, 1);
        Combatant enemyB = enemy("enemyB", 200, 5, 1);
        Combat combat = new Combat("ABC123", List.of(p1, enemyA, enemyB), List.of("p1", "enemyA", "enemyB"));

        combat.useAbility("p1", "test.cleave", null);

        // round(20 * 25/30) = 17 damage against 5 defense, applied to each alive enemy.
        assertThat(findCombatant(combat, "enemyA").currentHealth()).isEqualTo(200 - 17);
        assertThat(findCombatant(combat, "enemyB").currentHealth()).isEqualTo(200 - 17);
    }

    @Test
    void onDamageDealtHookHealsWearerByTheAmountDealt() {
        ItemPassive lifesteal = new ItemPassive() {
            @Override
            public void onDamageDealt(EffectTarget wearer, EffectTarget target, int amount) {
                wearer.applyHeal(amount);
            }
        };
        Combatant p1 = player("p1", List.of(STRIKE), List.of(lifesteal));
        Combatant p2 = player("p2", List.of(STRIKE));
        Combatant enemy = enemy("enemy", 200, 5, 50);
        Combat combat = twoPlayersOneEnemy(p1, p2, enemy);

        // Enemy's turn auto-resolves onto p1 (lowest health, tied, id tiebreak).
        combat.endTurn("p1");
        combat.endTurn("p2");
        // round(50 * 25/30) = 42 damage against defense 5.
        assertThat(findCombatant(combat, "p1").currentHealth()).isEqualTo(100 - 42);

        combat.useAbility("p1", "test.strike", "enemy");

        // round(20 * 25/30) = 17 damage dealt to the enemy; lifesteal heals p1 by that same amount.
        assertThat(findCombatant(combat, "enemy").currentHealth()).isEqualTo(200 - 17);
        assertThat(findCombatant(combat, "p1").currentHealth()).isEqualTo(100 - 42 + 17);
    }

    @Test
    void onDamageTakenHookReflectsDamageBackAtTheAttacker() {
        ItemPassive thorns = new ItemPassive() {
            @Override
            public void onDamageTaken(EffectTarget wearer, EffectTarget attacker, int amount) {
                attacker.applyDamage(amount);
            }
        };
        Combatant p1 = player("p1", List.of(STRIKE), List.of(thorns));
        Combatant p2 = player("p2", List.of(STRIKE));
        Combatant enemy = enemy("enemy", 200, 5, 50);
        Combat combat = twoPlayersOneEnemy(p1, p2, enemy);

        combat.endTurn("p1");
        combat.endTurn("p2");

        // round(50 * 25/30) = 42 damage taken by p1, reflected back onto the enemy in full.
        assertThat(findCombatant(combat, "p1").currentHealth()).isEqualTo(100 - 42);
        assertThat(findCombatant(combat, "enemy").currentHealth()).isEqualTo(200 - 42);
    }

    @Test
    void onKillFiresOnTheActorAndOnDeathFiresOnTheVictimForALethalHit() {
        List<String> log = new ArrayList<>();
        ItemPassive executioner = new ItemPassive() {
            @Override
            public void onKill(EffectTarget wearer, EffectTarget victim) {
                log.add("kill");
            }
        };
        ItemPassive lastGasp = new ItemPassive() {
            @Override
            public void onDeath(EffectTarget wearer) {
                log.add("death");
            }
        };
        Combatant p1 = player("p1", List.of(STRIKE), List.of(executioner));
        Combatant p2 = player("p2", List.of(STRIKE));
        Combatant enemy = enemy("enemy", 15, 0, 1, List.of(lastGasp));
        Combat combat = twoPlayersOneEnemy(p1, p2, enemy);

        combat.useAbility("p1", "test.strike", "enemy");

        assertThat(combat.status()).isEqualTo(CombatStatus.PARTY_WON);
        assertThat(log).containsExactly("kill", "death");
    }

    @Test
    void onDamageDealtAndOnKillDoNotFireForNonLethalMisses() {
        // A heal shouldn't be mistaken for damage dealt (health only ever goes up).
        List<String> log = new ArrayList<>();
        ItemPassive tattletale = new ItemPassive() {
            @Override
            public void onDamageDealt(EffectTarget wearer, EffectTarget target, int amount) {
                log.add("dealt");
            }
        };
        Combatant p1 = player("p1", List.of(MEND), List.of(tattletale));
        Combatant p2 = player("p2", List.of(STRIKE));
        Combatant enemy = enemy("enemy", 200, 5, 1);
        Combat combat = twoPlayersOneEnemy(p1, p2, enemy);

        combat.useAbility("p1", "test.mend", "p2");

        assertThat(log).isEmpty();
    }

    @Test
    void onStartTurnAndOnEndTurnFireForTheActingCombatantOnly() {
        List<String> log = new ArrayList<>();
        ItemPassive tracker = new ItemPassive() {
            @Override
            public void onStartTurn(EffectTarget wearer) {
                log.add("start");
            }

            @Override
            public void onEndTurn(EffectTarget wearer) {
                log.add("end");
            }
        };
        Combatant p1 = player("p1", List.of(STRIKE), List.of(tracker));
        Combatant p2 = player("p2", List.of(STRIKE));
        Combatant enemy = enemy("enemy", 200, 5, 1);
        Combat combat = twoPlayersOneEnemy(p1, p2, enemy);

        // No onStartTurn for p1 at combat creation — the turn engine only fires it via advanceTurn().
        assertThat(log).isEmpty();

        combat.endTurn("p1");
        assertThat(log).containsExactly("end");

        combat.endTurn("p2");
        // Enemy's turn auto-resolves (no tracker on it), then the cursor wraps back to p1.
        assertThat(log).containsExactly("end", "start");
    }
}
