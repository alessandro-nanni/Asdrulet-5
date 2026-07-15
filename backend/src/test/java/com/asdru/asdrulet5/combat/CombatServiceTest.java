package com.asdru.asdrulet5.combat;

import com.asdru.asdrulet5.classdata.ClassDefinitionRegistry;
import com.asdru.asdrulet5.classdata.domain.AbilityEffect;
import com.asdru.asdrulet5.classdata.domain.ActiveEffect;
import com.asdru.asdrulet5.classdata.domain.BasicAbility;
import com.asdru.asdrulet5.classdata.domain.TargetType;
import com.asdru.asdrulet5.combat.domain.Combat;
import com.asdru.asdrulet5.combat.domain.CombatStatus;
import com.asdru.asdrulet5.combat.domain.Combatant;
import com.asdru.asdrulet5.combat.exception.CombatNotFoundException;
import com.asdru.asdrulet5.combat.web.dto.CombatStateDto;
import com.asdru.asdrulet5.combat.web.dto.CombatantDto;
import com.asdru.asdrulet5.enemydata.EnemyDefinitionRegistry;
import com.asdru.asdrulet5.inventory.ItemDefinitionRegistry;
import com.asdru.asdrulet5.inventory.domain.ItemSlot;
import com.asdru.asdrulet5.inventory.domain.Loadout;
import com.asdru.asdrulet5.party.domain.CharacterClass;
import com.asdru.asdrulet5.party.domain.PartyMember;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
        return new PartyMember(id, id, null, characterClass, false, false, Loadout.empty(), currentHealth, List.of());
    }

    private static PartyMember memberWithPoison(String id, CharacterClass characterClass, int power, int turns) {
        ActiveEffect poison = ActiveEffect.damageOverTime("Poison", "A lingering venom saps your health each turn.",
                "poison", power, turns);
        return new PartyMember(id, id, null, characterClass, false, false, Loadout.empty(), null, List.of(poison));
    }

    @BeforeEach
    void setUp() {
        messagingTemplate = mock(SimpMessagingTemplate.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        combatService = new CombatService(new InMemoryCombatRepository(), new ClassDefinitionRegistry(false),
                new EnemyDefinitionRegistry(), new ItemDefinitionRegistry(), messagingTemplate, eventPublisher);
    }

    @Test
    void startCombatCreatesOneCombatantPerMemberPlusOneEnemyAndBroadcasts() {
        List<PartyMember> members = List.of(member("p1", CharacterClass.BERSERKER), member("p2", CharacterClass.HEALER));

        CombatStateDto dto = combatService.startCombat("ABC123", members, List.of("p1", "p2"));

        assertThat(dto.combatants()).hasSize(3);
        assertThat(dto.combatants().stream().filter(CombatantDto::enemy)).hasSize(1);
        assertThat(dto.status()).isEqualTo(CombatStatus.IN_PROGRESS);
        assertThat(dto.currentTurnCombatantId()).isEqualTo("p1");
        verify(messagingTemplate, times(1)).convertAndSend("/topic/party/ABC123/combat", dto);
    }

    @Test
    void getUnknownCombatThrows() {
        assertThatThrownBy(() -> combatService.getState("NOPE99"))
                .isInstanceOf(CombatNotFoundException.class);
    }

    @Test
    void partyCombatantsForReturnsOnlyTheNonEnemyCombatantsInTheirCurrentState() {
        List<PartyMember> members = List.of(member("p1", CharacterClass.BERSERKER), member("p2", CharacterClass.HEALER));
        combatService.startCombat("ABC123", members, List.of("p1", "p2"));
        String enemyId = combatService.getState("ABC123").combatants().stream()
                .filter(CombatantDto::enemy).findFirst().orElseThrow().id();
        combatService.useAbility("ABC123", "p1", "berserker.reckless-strike", enemyId);

        List<Combatant> partyCombatants = combatService.partyCombatantsFor("ABC123");

        assertThat(partyCombatants).extracting(Combatant::id).containsExactlyInAnyOrder("p1", "p2");
        assertThat(partyCombatants).noneMatch(Combatant::enemy);
    }

    @Test
    void useAbilityAppliesEffectAndBroadcasts() {
        List<PartyMember> members = List.of(member("p1", CharacterClass.BERSERKER), member("p2", CharacterClass.HEALER));
        CombatStateDto started = combatService.startCombat("ABC123", members, List.of("p1", "p2"));
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

        CombatStateDto dto = combatService.startCombat("ABC123", members, List.of("p1"));

        CombatantDto mage = dto.combatants().stream().filter(c -> c.id().equals("p1")).findFirst().orElseThrow();
        assertThat(mage.maxHealth()).isEqualTo(70);
        assertThat(mage.defense()).isEqualTo(12);
    }

    @Test
    void scytheIncreasesDamageByThirteenPercentForEachDeadAlly() {
        Loadout loadout = Loadout.empty().withItem(ItemSlot.WEAPON, "scythe");
        PartyMember alive = member("p1", CharacterClass.MAGE, loadout);
        PartyMember dead = memberWithHealth("p2", CharacterClass.BERSERKER, 0);
        List<PartyMember> members = List.of(alive, dead);

        CombatStateDto dto = combatService.startCombat("ABC123", members, List.of("p1", "p2"));
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

        CombatStateDto dto = combatService.startCombat("ABC123", members, List.of("p1"));

        CombatantDto berserker = dto.combatants().stream().filter(c -> c.id().equals("p1")).findFirst().orElseThrow();
        assertThat(berserker.maxHealth()).isEqualTo(80);
        assertThat(berserker.currentHealth()).isEqualTo(30);
    }

    @Test
    void startCombatWithNoCarriedOverHealthStartsAtMax() {
        List<PartyMember> members = List.of(member("p1", CharacterClass.BERSERKER));

        CombatStateDto dto = combatService.startCombat("ABC123", members, List.of("p1"));

        CombatantDto berserker = dto.combatants().stream().filter(c -> c.id().equals("p1")).findFirst().orElseThrow();
        assertThat(berserker.currentHealth()).isEqualTo(berserker.maxHealth());
    }

    @Test
    void startCombatSeedsAPendingPoisonAsAnActiveEffect() {
        List<PartyMember> members = List.of(memberWithPoison("p1", CharacterClass.BERSERKER, 6, 4));

        CombatStateDto dto = combatService.startCombat("ABC123", members, List.of("p1"));

        CombatantDto berserker = dto.combatants().stream().filter(c -> c.id().equals("p1")).findFirst().orElseThrow();
        assertThat(berserker.activeEffects()).hasSize(1);
        assertThat(berserker.activeEffects().getFirst().name()).isEqualTo("Poison");
        assertThat(berserker.activeEffects().getFirst().remainingTurns()).isEqualTo(4);
    }

    @Test
    void endTurnAdvancesToNextCombatant() {
        List<PartyMember> members = List.of(member("p1", CharacterClass.BERSERKER), member("p2", CharacterClass.HEALER));
        combatService.startCombat("ABC123", members, List.of("p1", "p2"));

        CombatStateDto updated = combatService.endTurn("ABC123", "p1");

        assertThat(updated.currentTurnCombatantId()).isEqualTo("p2");
    }

    @Test
    void useAbilityThatDefeatsTheLastEnemyPublishesVictoryEvent() {
        BasicAbility strike = new BasicAbility(
                "test.strike", "Strike", "A basic attack.", "20 damage", TargetType.SINGLE_ENEMY, 10,
                AbilityEffect.damage(20));
        Combatant p1 = new Combatant(
                "p1", "p1", false, CharacterClass.BERSERKER, 100, 100, 5, 0, 40, List.of(strike), null, null, null,
                null, List.of());
        Combatant weakEnemy = new Combatant(
                "enemy-1", "enemy-1", true, null, 1, 0, 0, 0, 0, List.of(), "Claw", "A swipe.", "5 damage",
                AbilityEffect.damage(5), List.of());
        Combat combat = new Combat("VICT123", List.of(p1, weakEnemy), List.of("p1", "enemy-1"));
        CombatRepository repository = new InMemoryCombatRepository();
        repository.save(combat);
        CombatService service = new CombatService(repository, new ClassDefinitionRegistry(false),
                new EnemyDefinitionRegistry(), new ItemDefinitionRegistry(), messagingTemplate, eventPublisher);

        CombatStateDto updated = service.useAbility("VICT123", "p1", "test.strike", "enemy-1");

        assertThat(updated.status()).isEqualTo(CombatStatus.PARTY_WON);
        verify(eventPublisher, times(1)).publishEvent(new CombatVictoryEvent("VICT123"));
    }
}
