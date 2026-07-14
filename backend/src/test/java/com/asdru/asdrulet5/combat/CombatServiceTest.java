package com.asdru.asdrulet5.combat;

import com.asdru.asdrulet5.classdata.ClassDefinitionRegistry;
import com.asdru.asdrulet5.classdata.domain.AbilityEffect;
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
        return new PartyMember(id, id, null, characterClass, false, false, loadout);
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
        List<PartyMember> members = List.of(member("p1", CharacterClass.WARRIOR), member("p2", CharacterClass.HEALER));

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
    void useAbilityAppliesEffectAndBroadcasts() {
        List<PartyMember> members = List.of(member("p1", CharacterClass.WARRIOR), member("p2", CharacterClass.HEALER));
        CombatStateDto started = combatService.startCombat("ABC123", members, List.of("p1", "p2"));
        String enemyId = started.combatants().stream().filter(CombatantDto::enemy).findFirst().orElseThrow().id();
        int enemyHealthBefore = started.combatants().stream()
                .filter(c -> c.id().equals(enemyId)).findFirst().orElseThrow().currentHealth();

        CombatStateDto updated = combatService.useAbility("ABC123", "p1", "warrior.cleave", enemyId);

        int enemyHealthAfter = updated.combatants().stream()
                .filter(c -> c.id().equals(enemyId)).findFirst().orElseThrow().currentHealth();
        assertThat(enemyHealthAfter).isLessThan(enemyHealthBefore);
        verify(messagingTemplate, times(2)).convertAndSend(eq("/topic/party/ABC123/combat"), any(CombatStateDto.class));
    }

    @Test
    void startCombatAppliesEquippedItemPassiveBonusesToStats() {
        // Warrior base stats: 120 health, 18 damage, 10 defense (see WarriorClassDefinition).
        Loadout loadout = Loadout.empty()
                .withItem(ItemSlot.WEAPON, "flame-edge")      // +7 damage
                .withItem(ItemSlot.CHESTPLATE, "plate-armor"); // +20 health, +6 defense
        List<PartyMember> members = List.of(member("p1", CharacterClass.WARRIOR, loadout));

        CombatStateDto dto = combatService.startCombat("ABC123", members, List.of("p1"));

        CombatantDto warrior = dto.combatants().stream().filter(c -> c.id().equals("p1")).findFirst().orElseThrow();
        assertThat(warrior.maxHealth()).isEqualTo(140);
        assertThat(warrior.damage()).isEqualTo(25);
        assertThat(warrior.defense()).isEqualTo(16);
    }

    @Test
    void endTurnAdvancesToNextCombatant() {
        List<PartyMember> members = List.of(member("p1", CharacterClass.WARRIOR), member("p2", CharacterClass.HEALER));
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
                "p1", "p1", false, CharacterClass.WARRIOR, 100, 100, 5, 10, 40, List.of(strike), null, null, null,
                null, List.of());
        Combatant weakEnemy = new Combatant(
                "enemy-1", "enemy-1", true, null, 1, 0, 0, 5, 0, List.of(), "Claw", "A swipe.", "5 damage",
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
