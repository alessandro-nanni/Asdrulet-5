package com.asdru.asdrulet5.combat;

import com.asdru.asdrulet5.classdata.ClassDefinitionRegistry;
import com.asdru.asdrulet5.classdata.domain.*;
import com.asdru.asdrulet5.combat.domain.*;
import com.asdru.asdrulet5.combat.exception.CombatNotFoundException;
import com.asdru.asdrulet5.combat.web.dto.CombatStateDto;
import com.asdru.asdrulet5.combat.web.dto.CombatantDto;
import com.asdru.asdrulet5.enemydata.EnemyDefinitionRegistry;
import com.asdru.asdrulet5.enemydata.EnemyEncounterRegistry;
import com.asdru.asdrulet5.inventory.ItemDefinitionRegistry;
import com.asdru.asdrulet5.inventory.domain.ItemSlot;
import com.asdru.asdrulet5.inventory.domain.Loadout;
import com.asdru.asdrulet5.party.domain.CharacterClass;
import com.asdru.asdrulet5.party.domain.Party;
import com.asdru.asdrulet5.party.domain.PartyMember;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class CombatServiceTest {

    private CombatService combatService;
    private SimpMessagingTemplate messagingTemplate;
    private ApplicationEventPublisher eventPublisher;

    private static PartyMember member(String id, CharacterClass characterClass) {
        return member(id, characterClass, Loadout.empty());
    }

    private static PartyMember member(String id, CharacterClass characterClass, Loadout loadout) {
        return new PartyMember(id, id, null, characterClass, false, false, loadout, null, List.of());
    }

    private static PartyMember memberWithHealth(String id, CharacterClass characterClass, Integer currentHealth) {
        return memberWithHealth(id, characterClass, currentHealth, Loadout.empty());
    }

    private static PartyMember memberWithHealth(String id, CharacterClass characterClass, Integer currentHealth, Loadout loadout) {
        return new PartyMember(id, id, null, characterClass, false, false, loadout, currentHealth, List.of());
    }

    private static PartyMember memberWithPoison(String id, CharacterClass characterClass, int power, int turns) {
        ActiveEffect poison = ActiveEffect.damageOverTime("Poison", "A lingering venom saps your health each turn.",
                "poison", power, turns);
        return new PartyMember(id, id, null, characterClass, false, false, Loadout.empty(), null, List.of(poison));
    }

    /**
     * A bare, never-"started" Party — enough for CombatService to hand each
     * PlayerCombatant a real reference to belong to (see PlayerCombatant.party())
     * without any test having to also satisfy Party's own lobby-state-machine
     * invariants (start(), selectClass(), ...), which combat has no stake in.
     */
    private static Party party(String code) {
        return new Party(code, "leader", "Leader", null);
    }

    @BeforeEach
    void setUp() {
        messagingTemplate = mock(SimpMessagingTemplate.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        combatService = new CombatService(new InMemoryCombatRepository(), new ClassDefinitionRegistry(false),
                new EnemyDefinitionRegistry(), new EnemyEncounterRegistry(), new ItemDefinitionRegistry(),
                messagingTemplate, eventPublisher, new EnemyActionDelay(0));
    }

    @Test
    void startCombatCreatesOneCombatantPerMemberPlusOneBossAndBroadcasts() {
        List<PartyMember> members = List.of(member("p1", CharacterClass.BERSERKER), member("p2", CharacterClass.HEALER));

        CombatStateDto dto = combatService.startCombat(party("ABC123"), members, List.of("p1", "p2"), true);

        assertThat(dto.combatants()).hasSize(3);
        assertThat(dto.combatants().stream().filter(CombatantDto::enemy)).hasSize(1);
        assertThat(dto.status()).isEqualTo(CombatStatus.IN_PROGRESS);
        assertThat(dto.currentTurnCombatantId()).isEqualTo("p1");
        verify(messagingTemplate, times(1)).convertAndSend("/topic/party/ABC123/combat", dto);
    }

    @Test
    void startCombatForARegularFightSpawnsTwoOrThreeEnemiesFromThePool() {
        List<PartyMember> members = List.of(member("p1", CharacterClass.BERSERKER), member("p2", CharacterClass.HEALER));

        CombatStateDto dto = combatService.startCombat(party("ABC123"), members, List.of("p1", "p2"), false);

        List<CombatantDto> enemies = dto.combatants().stream().filter(CombatantDto::enemy).toList();
        assertThat(enemies.size()).isBetween(2, 3);
        assertThat(enemies).extracting(CombatantDto::id).doesNotHaveDuplicates();
        // enemyDefinitionId (unlike id/displayName) is the stable per-species
        // key the frontend uses to look up a portrait — every regular-fight
        // enemy should carry a real, non-boss id.
        assertThat(enemies).extracting(CombatantDto::enemyDefinitionId)
                .allSatisfy(id -> assertThat(id).isNotBlank());
        assertThat(enemies).extracting(CombatantDto::enemyDefinitionId)
                .doesNotContain(EnemyDefinitionRegistry.DEFAULT_ENEMY_ID);
        // Every enemy turn is appended to the sequence, so the party still goes first.
        assertThat(dto.currentTurnCombatantId()).isEqualTo("p1");
    }

    @Test
    void startCombatForABossFightAlwaysSpawnsExactlyOneGoblinMarauder() {
        List<PartyMember> members = List.of(member("p1", CharacterClass.BERSERKER));

        for (int i = 0; i < 20; i++) {
            CombatStateDto dto = combatService.startCombat(party("BOSS" + i), members, List.of("p1"), true);

            List<CombatantDto> enemies = dto.combatants().stream().filter(CombatantDto::enemy).toList();
            assertThat(enemies).hasSize(1);
            assertThat(enemies.getFirst().displayName()).isEqualTo("Goblin Marauder");
            assertThat(enemies.getFirst().enemyDefinitionId()).isEqualTo(EnemyDefinitionRegistry.DEFAULT_ENEMY_ID);
        }
    }

    @Test
    void partyMemberCombatantsHaveNoEnemyDefinitionId() {
        List<PartyMember> members = List.of(member("p1", CharacterClass.BERSERKER));

        CombatStateDto dto = combatService.startCombat(party("ABC123"), members, List.of("p1"), true);

        CombatantDto p1 = dto.combatants().stream().filter(c -> c.id().equals("p1")).findFirst().orElseThrow();
        assertThat(p1.enemyDefinitionId()).isNull();
    }

    @Test
    void getUnknownCombatThrows() {
        assertThatThrownBy(() -> combatService.getState("NOPE99"))
                .isInstanceOf(CombatNotFoundException.class);
    }

    @Test
    void partyCombatantsForReturnsOnlyTheNonEnemyCombatantsInTheirCurrentState() {
        List<PartyMember> members = List.of(member("p1", CharacterClass.BERSERKER), member("p2", CharacterClass.HEALER));
        combatService.startCombat(party("ABC123"), members, List.of("p1", "p2"), true);
        String enemyId = combatService.getState("ABC123").combatants().stream()
                .filter(CombatantDto::enemy).findFirst().orElseThrow().id();
        combatService.useAbility("ABC123", "p1", "berserker.reckless-strike", enemyId);

        List<Combatant> partyCombatants = combatService.partyCombatantsFor("ABC123");

        assertThat(partyCombatants).extracting(Combatant::combatantId).containsExactlyInAnyOrder("p1", "p2");
        assertThat(partyCombatants).noneMatch(Combatant::enemy);
    }

    @Test
    void useAbilityAppliesEffectAndBroadcasts() {
        List<PartyMember> members = List.of(member("p1", CharacterClass.BERSERKER), member("p2", CharacterClass.HEALER));
        CombatStateDto started = combatService.startCombat(party("ABC123"), members, List.of("p1", "p2"), true);
        String enemyId = started.combatants().stream().filter(CombatantDto::enemy).findFirst().orElseThrow().id();
        int enemyHealthBefore = started.combatants().stream()
                .filter(c -> c.id().equals(enemyId)).findFirst().orElseThrow().currentHealth();

        CombatStateDto updated = combatService.useAbility("ABC123", "p1", "berserker.reckless-strike", enemyId);

        int enemyHealthAfter = updated.combatants().stream()
                .filter(c -> c.id().equals(enemyId)).findFirst().orElseThrow().currentHealth();
        assertThat(enemyHealthAfter).isLessThan(enemyHealthBefore);
        verify(messagingTemplate, times(2)).convertAndSend(eq("/topic/party/ABC123/combat"), any(CombatStateDto.class));
    }

    @Test
    void startCombatAppliesEquippedItemPassiveBonusesToStats() {
        // Mage base stats: 70 health, 2 defense (see MageClassDefinition).
        Loadout loadout = Loadout.empty()
                .withItem(ItemSlot.TRINKET, "satellite-dish")   // +6 defense
                .withItem(ItemSlot.CHESTPLATE, "leather-tunic"); // +4 defense
        List<PartyMember> members = List.of(member("p1", CharacterClass.MAGE, loadout));

        CombatStateDto dto = combatService.startCombat(party("ABC123"), members, List.of("p1"), true);

        CombatantDto mage = dto.combatants().stream().filter(c -> c.id().equals("p1")).findFirst().orElseThrow();
        assertThat(mage.maxHealth()).isEqualTo(70);
        assertThat(mage.defense()).isEqualTo(12);
    }

    @Test
    void mantleOfTheUsurperBoostsMaxHealthWhileHealthierThanTheLeader() {
        // Mage base max health 70 (see MageClassDefinition) + 7% = 74 (int
        // division rounds 4.9 down to 4).
        Loadout mantle = Loadout.empty().withItem(ItemSlot.CHESTPLATE, "mantle-of-the-usurper");
        PartyMember lowHealthLeader = memberWithHealth("leader", CharacterClass.BERSERKER, 10);
        PartyMember wearer = member("wearer", CharacterClass.MAGE, mantle);
        Party party = new Party("ABC123", "leader", "Leader", null);

        CombatStateDto dto = combatService.startCombat(party, List.of(lowHealthLeader, wearer),
                List.of("leader", "wearer"), true);

        CombatantDto wearerDto = dto.combatants().stream().filter(c -> c.id().equals("wearer")).findFirst().orElseThrow();
        assertThat(wearerDto.maxHealth()).isEqualTo(74);
        // Started at full (no carried-over health) — the bonus should grow current health right along with the new max.
        assertThat(wearerDto.currentHealth()).isEqualTo(74);
    }

    @Test
    void mantleOfTheUsurperGrantsNoBonusWhenNotHealthierThanTheLeader() {
        Loadout mantle = Loadout.empty().withItem(ItemSlot.CHESTPLATE, "mantle-of-the-usurper");
        PartyMember fullHealthLeader = member("leader", CharacterClass.BERSERKER);
        PartyMember lowHealthWearer = memberWithHealth("wearer", CharacterClass.MAGE, 5, mantle);
        Party party = new Party("ABC123", "leader", "Leader", null);

        CombatStateDto dto = combatService.startCombat(party, List.of(fullHealthLeader, lowHealthWearer),
                List.of("leader", "wearer"), true);

        CombatantDto wearerDto = dto.combatants().stream().filter(c -> c.id().equals("wearer")).findFirst().orElseThrow();
        assertThat(wearerDto.maxHealth()).isEqualTo(70);
        assertThat(wearerDto.currentHealth()).isEqualTo(5);
    }

    @Test
    void mantleOfTheUsurperGrantsALiveFivePercentDamageBonusWhileHealthierThanTheLeader() {
        // Unlike the max-health slice (baked into Stats once, at fight
        // start), this half of the bonus is resolved live off the wearer —
        // asserted directly against Combatant.damagePercentBonus() rather
        // than through an actual ability's rounded, defense-mitigated (and
        // for some abilities, crit-randomized) damage output, which isn't a
        // reliable way to observe a specific percentage.
        Loadout mantle = Loadout.empty().withItem(ItemSlot.CHESTPLATE, "mantle-of-the-usurper");
        PartyMember lowHealthLeader = memberWithHealth("leader", CharacterClass.BERSERKER, 10);
        PartyMember wearer = member("wearer", CharacterClass.MAGE, mantle);
        Party party = new Party("ABC123", "leader", "Leader", null);

        combatService.startCombat(party, List.of(lowHealthLeader, wearer), List.of("leader", "wearer"), true);

        Combatant wearerCombatant = combatService.partyCombatantsFor("ABC123").stream()
                .filter(combatant -> combatant.combatantId().equals("wearer")).findFirst().orElseThrow();
        assertThat(wearerCombatant.damagePercentBonus()).isEqualTo(5);
    }

    @Test
    void scytheIncreasesDamageByThirteenPercentForEachDeadAlly() {
        Loadout loadout = Loadout.empty().withItem(ItemSlot.WEAPON, "scythe");
        PartyMember alive = member("p1", CharacterClass.MAGE, loadout);
        PartyMember dead = memberWithHealth("p2", CharacterClass.BERSERKER, 0);
        List<PartyMember> members = List.of(alive, dead);

        CombatStateDto dto = combatService.startCombat(party("ABC123"), members, List.of("p1", "p2"), true);
        String enemyId = dto.combatants().stream().filter(CombatantDto::enemy).findFirst().orElseThrow().id();
        int enemyHealthBefore = dto.combatants().stream()
                .filter(c -> c.id().equals(enemyId)).findFirst().orElseThrow().currentHealth();

        CombatStateDto updated = combatService.useAbility("ABC123", "p1", "mage.arcane-blast", enemyId);

        int enemyHealthAfter = updated.combatants().stream()
                .filter(c -> c.id().equals(enemyId)).findFirst().orElseThrow().currentHealth();
        // Arcane Blast's 9 power scaled by Scythe's +13% (one dead ally) = round(9 * 1.13) = 10,
        // mitigated by the goblin's 8 defense: round(10 * (1 - 8/33)) = 8.
        assertThat(enemyHealthBefore - enemyHealthAfter).isEqualTo(8);
    }

    @Test
    void startCombatHonorsAMemberSCarriedOverCurrentHealth() {
        // A MYSTERY wheel's "halve current health" persists on the member
        // until the next fight actually starts — see PartyMember's own doc.
        List<PartyMember> members = List.of(memberWithHealth("p1", CharacterClass.BERSERKER, 30));

        CombatStateDto dto = combatService.startCombat(party("ABC123"), members, List.of("p1"), true);

        CombatantDto berserker = dto.combatants().stream().filter(c -> c.id().equals("p1")).findFirst().orElseThrow();
        assertThat(berserker.maxHealth()).isEqualTo(80);
        assertThat(berserker.currentHealth()).isEqualTo(30);
    }

    @Test
    void startCombatWithNoCarriedOverHealthStartsAtMax() {
        List<PartyMember> members = List.of(member("p1", CharacterClass.BERSERKER));

        CombatStateDto dto = combatService.startCombat(party("ABC123"), members, List.of("p1"), true);

        CombatantDto berserker = dto.combatants().stream().filter(c -> c.id().equals("p1")).findFirst().orElseThrow();
        assertThat(berserker.currentHealth()).isEqualTo(berserker.maxHealth());
    }

    @Test
    void startCombatSeedsAPendingPoisonAsAnActiveEffect() {
        List<PartyMember> members = List.of(memberWithPoison("p1", CharacterClass.BERSERKER, 6, 4));

        CombatStateDto dto = combatService.startCombat(party("ABC123"), members, List.of("p1"), true);

        CombatantDto berserker = dto.combatants().stream().filter(c -> c.id().equals("p1")).findFirst().orElseThrow();
        assertThat(berserker.activeEffects()).hasSize(1);
        assertThat(berserker.activeEffects().getFirst().name()).isEqualTo("Poison");
        assertThat(berserker.activeEffects().getFirst().remainingTurns()).isEqualTo(4);
    }

    @Test
    void endTurnAdvancesToNextCombatant() {
        List<PartyMember> members = List.of(member("p1", CharacterClass.BERSERKER), member("p2", CharacterClass.HEALER));
        combatService.startCombat(party("ABC123"), members, List.of("p1", "p2"), true);

        CombatStateDto updated = combatService.endTurn("ABC123", "p1");

        assertThat(updated.currentTurnCombatantId()).isEqualTo("p2");
    }

    @Test
    void endTurnBroadcastsSeparatelyForEachEnemyInACascadeWithTheirOwnIdCurrent() {
        BasicAbility weakAttack = new BasicAbility(
                "test-enemy.claw", "Claw", "A swipe.", "1 damage", TargetType.SINGLE_ENEMY, 0,
                AbilityEffect.damage(1));
        Combatant p1 = new PlayerCombatant(
                "p1", "p1", CharacterClass.BERSERKER, new Stats(100, 0, 100), 100, List.of(), List.of(), party("ABC123"));
        Combatant enemyA = new EnemyCombatant(
                "enemyA", "enemyA", "test-enemy", new Stats(50, 0, 0), 0, List.of(weakAttack), List.of());
        Combatant enemyB = new EnemyCombatant(
                "enemyB", "enemyB", "test-enemy", new Stats(50, 0, 0), 0, List.of(weakAttack), List.of());
        Combat combat = new Combat("ABC123", List.of(p1, enemyA, enemyB), List.of("p1", "enemyA", "enemyB"));
        CombatRepository repository = new InMemoryCombatRepository();
        repository.save(combat);
        CombatService service = new CombatService(repository, new ClassDefinitionRegistry(false),
                new EnemyDefinitionRegistry(), new EnemyEncounterRegistry(), new ItemDefinitionRegistry(),
                messagingTemplate, eventPublisher, new EnemyActionDelay(0));

        CombatStateDto updated = service.endTurn("ABC123", "p1");

        // One broadcast per enemy action, plus one more once control hands
        // back to p1 — not a single lump-sum broadcast covering both
        // enemies' attacks at once.
        ArgumentCaptor<CombatStateDto> captor = ArgumentCaptor.forClass(CombatStateDto.class);
        verify(messagingTemplate, times(3)).convertAndSend(eq("/topic/party/ABC123/combat"), captor.capture());
        List<CombatStateDto> broadcasts = captor.getAllValues();
        assertThat(broadcasts.get(0).currentTurnCombatantId()).isEqualTo("enemyA");
        assertThat(broadcasts.get(1).currentTurnCombatantId()).isEqualTo("enemyB");
        assertThat(broadcasts.get(2).currentTurnCombatantId()).isEqualTo("p1");
        assertThat(updated.currentTurnCombatantId()).isEqualTo("p1");
    }

    @Test
    void endTurnPausesBetweenEachEnemysActionButNotAfterTheLast() {
        BasicAbility weakAttack = new BasicAbility(
                "test-enemy.claw", "Claw", "A swipe.", "1 damage", TargetType.SINGLE_ENEMY, 0,
                AbilityEffect.damage(1));
        Combatant p1 = new PlayerCombatant(
                "p1", "p1", CharacterClass.BERSERKER, new Stats(100, 0, 100), 100, List.of(), List.of(), party("ABC123"));
        Combatant enemyA = new EnemyCombatant(
                "enemyA", "enemyA", "test-enemy", new Stats(50, 0, 0), 0, List.of(weakAttack), List.of());
        Combatant enemyB = new EnemyCombatant(
                "enemyB", "enemyB", "test-enemy", new Stats(50, 0, 0), 0, List.of(weakAttack), List.of());
        Combat combat = new Combat("ABC123", List.of(p1, enemyA, enemyB), List.of("p1", "enemyA", "enemyB"));
        CombatRepository repository = new InMemoryCombatRepository();
        repository.save(combat);
        EnemyActionDelay mockDelay = mock(EnemyActionDelay.class);
        CombatService service = new CombatService(repository, new ClassDefinitionRegistry(false),
                new EnemyDefinitionRegistry(), new EnemyEncounterRegistry(), new ItemDefinitionRegistry(),
                messagingTemplate, eventPublisher, mockDelay);

        service.endTurn("ABC123", "p1");

        // Paced after enemyA's action and after enemyB's, but not once more
        // after control hands back to p1 — nothing left to wait on there.
        verify(mockDelay, times(2)).sleep();
    }

    @Test
    void useAbilityThatDefeatsTheLastEnemyPublishesVictoryEvent() {
        BasicAbility strike = new BasicAbility(
                "test.strike", "Strike", "A basic attack.", "20 damage", TargetType.SINGLE_ENEMY, 10,
                AbilityEffect.damage(20));
        Combatant p1 = new PlayerCombatant(
                "p1", "p1", CharacterClass.BERSERKER, new Stats(100, 5, 100), 40, List.of(strike), List.of(), party("VICT123"));
        BasicAbility weakEnemyAttack = new BasicAbility(
                "test-enemy.claw", "Claw", "A swipe.", "5 damage", TargetType.SINGLE_ENEMY, 0,
                AbilityEffect.damage(5));
        Combatant weakEnemy = new EnemyCombatant(
                "enemy-1", "enemy-1", "test-enemy", new Stats(1, 0, 0), 0, List.of(weakEnemyAttack), List.of());
        Combat combat = new Combat("VICT123", List.of(p1, weakEnemy), List.of("p1", "enemy-1"));
        CombatRepository repository = new InMemoryCombatRepository();
        repository.save(combat);
        CombatService service = new CombatService(repository, new ClassDefinitionRegistry(false),
                new EnemyDefinitionRegistry(), new EnemyEncounterRegistry(), new ItemDefinitionRegistry(),
                messagingTemplate, eventPublisher, new EnemyActionDelay(0));

        CombatStateDto updated = service.useAbility("VICT123", "p1", "test.strike", "enemy-1");

        assertThat(updated.status()).isEqualTo(CombatStatus.PARTY_WON);
        verify(eventPublisher, times(1)).publishEvent(new CombatVictoryEvent("VICT123"));
    }
}
