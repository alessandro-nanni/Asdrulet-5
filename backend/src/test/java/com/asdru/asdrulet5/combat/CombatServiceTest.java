package com.asdru.asdrulet5.combat;

import com.asdru.asdrulet5.classdata.ClassDefinitionRegistry;
import com.asdru.asdrulet5.combat.domain.CombatStatus;
import com.asdru.asdrulet5.combat.exception.CombatNotFoundException;
import com.asdru.asdrulet5.combat.web.dto.CombatStateDto;
import com.asdru.asdrulet5.combat.web.dto.CombatantDto;
import com.asdru.asdrulet5.enemydata.EnemyDefinitionRegistry;
import com.asdru.asdrulet5.party.domain.CharacterClass;
import com.asdru.asdrulet5.party.domain.PartyMember;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

    private static PartyMember member(String id, CharacterClass characterClass) {
        return new PartyMember(id, id, null, characterClass, false, false);
    }

    @BeforeEach
    void setUp() {
        messagingTemplate = mock(SimpMessagingTemplate.class);
        combatService = new CombatService(new InMemoryCombatRepository(), new ClassDefinitionRegistry(),
                new EnemyDefinitionRegistry(), messagingTemplate);
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
    void endTurnAdvancesToNextCombatant() {
        List<PartyMember> members = List.of(member("p1", CharacterClass.WARRIOR), member("p2", CharacterClass.HEALER));
        combatService.startCombat("ABC123", members, List.of("p1", "p2"));

        CombatStateDto updated = combatService.endTurn("ABC123", "p1");

        assertThat(updated.currentTurnCombatantId()).isEqualTo("p2");
    }
}
