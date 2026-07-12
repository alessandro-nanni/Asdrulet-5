package com.asdru.asdrulet5.combat.domain;

import com.asdru.asdrulet5.classdata.domain.*;
import com.asdru.asdrulet5.combat.exception.CombatNotInProgressException;
import com.asdru.asdrulet5.combat.exception.InsufficientResourceException;
import com.asdru.asdrulet5.combat.exception.NotYourTurnException;
import com.asdru.asdrulet5.party.domain.CharacterClass;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CombatTest {

    private static final BasicAbility STRIKE = new BasicAbility(
            "test.strike", "Strike", "A basic attack.", TargetType.SINGLE_ENEMY, 30,
            new DamageEffect(20));

    private static final BasicAbility EXPENSIVE_STRIKE = new BasicAbility(
            "test.expensive-strike", "Expensive Strike", "A costly attack.", TargetType.SINGLE_ENEMY, 90,
            new DamageEffect(20));

    private static final BasicAbility MEND = new BasicAbility(
            "test.mend", "Mend", "A basic heal.", TargetType.SINGLE_ALLY, 10,
            new HealEffect(15));

    private static final BasicAbility BRACE = new BasicAbility(
            "test.brace", "Brace", "Raises defense.", TargetType.SELF, 10,
            new BuffDefenseEffect(10, 1));

    private static final BasicAbility PUMP_UP = new BasicAbility(
            "test.pump-up", "Pump Up", "Raises damage.", TargetType.SELF, 10,
            new BuffDamageEffect(15, 2));

    private static final UltimateAbility ULTIMATE_STRIKE = new UltimateAbility(
            "test.ultimate-strike", "Ultimate Strike", "A big attack.", TargetType.SINGLE_ENEMY, 40,
            new DamageEffect(50));

    private static Combatant player(String id, List<Ability> abilities) {
        return new Combatant(id, id, false, CharacterClass.WARRIOR, 100, 100, 5, 40, abilities, null, null, null);
    }

    private static Combatant enemy(String id, int maxHealth, int defense, int attackPower) {
        return new Combatant(id, id, true, null, maxHealth, 0, defense, 0, List.of(),
                "Claw", "A swipe.", new DamageEffect(attackPower));
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
        assertThat(updatedEnemy.currentHealth()).isEqualTo(200 - (20 - 6));
        assertThat(updatedP1.currentStamina()).isEqualTo(100 - 30);
    }

    @Test
    void damageIsClampedToAtLeastOne() {
        Combatant p1 = player("p1", List.of(STRIKE, MEND, ULTIMATE_STRIKE));
        Combatant p2 = player("p2", List.of(STRIKE, MEND, ULTIMATE_STRIKE));
        Combatant enemy = enemy("enemy", 200, 999, 10);
        Combat combat = twoPlayersOneEnemy(p1, p2, enemy);

        combat.useAbility("p1", "test.strike", "enemy");

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

        // Each strike deals 15 damage (20 power - 5 defense); charge threshold is 40.
        combat.useAbility("p1", "test.strike", "enemy");
        assertThatThrownBy(() -> combat.useAbility("p1", "test.ultimate-strike", "enemy"))
                .isInstanceOf(InsufficientResourceException.class);

        combat.useAbility("p1", "test.strike", "enemy");
        assertThat(findCombatant(combat, "p1").ultimateCharge()).isEqualTo(30);

        combat.useAbility("p1", "test.strike", "enemy");
        assertThat(findCombatant(combat, "p1").ultimateCharge()).isEqualTo(40);

        combat.useAbility("p1", "test.ultimate-strike", "enemy");
        // The ultimate's own damage (50 - 5 = 45) immediately re-adds charge, capped at threshold.
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
        assertThat(lowestHealthPlayer.currentHealth()).isEqualTo(100 - (12 - 5));
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
        // Enemy attacked p1 (lowest HP tiebreak) while Brace (defense +10, 1 turn) was active: 20 - (5+10) = 5 dmg.
        assertThat(findCombatant(combat, "p1").currentHealth()).isEqualTo(95);

        // Brace had 1 turn remaining; it should have expired when p1's turn started again.
        combat.endTurn("p1");
        combat.endTurn("p2");
        // Second enemy attack with no buff active: 20 - 5 = 15 dmg, on whichever player has lowest HP (p1, at 95).
        assertThat(findCombatant(combat, "p1").currentHealth()).isEqualTo(95 - 15);
    }

    @Test
    void buffDamageIncreasesOutgoingDamage() {
        Combatant p1 = player("p1", List.of(PUMP_UP, STRIKE));
        Combatant p2 = player("p2", List.of(STRIKE));
        Combatant enemy = enemy("enemy", 200, 5, 1);
        Combat combat = twoPlayersOneEnemy(p1, p2, enemy);

        combat.useAbility("p1", "test.pump-up", "p1");
        combat.useAbility("p1", "test.strike", "enemy");

        // 20 (strike power) + 15 (buff) - 5 (defense) = 30.
        assertThat(findCombatant(combat, "enemy").currentHealth()).isEqualTo(200 - 30);
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
                "test.cleave", "Cleave", "Hits every enemy.", TargetType.ALL_ENEMIES, 10,
                new DamageEffect(20));
        Combatant p1 = player("p1", List.of(cleave));
        Combatant enemyA = enemy("enemyA", 200, 5, 1);
        Combatant enemyB = enemy("enemyB", 200, 5, 1);
        Combat combat = new Combat("ABC123", List.of(p1, enemyA, enemyB), List.of("p1", "enemyA", "enemyB"));

        combat.useAbility("p1", "test.cleave", null);

        assertThat(findCombatant(combat, "enemyA").currentHealth()).isEqualTo(200 - 15);
        assertThat(findCombatant(combat, "enemyB").currentHealth()).isEqualTo(200 - 15);
    }
}
