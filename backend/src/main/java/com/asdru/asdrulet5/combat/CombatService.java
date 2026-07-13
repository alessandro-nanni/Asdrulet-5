package com.asdru.asdrulet5.combat;

import com.asdru.asdrulet5.classdata.ClassDefinitionRegistry;
import com.asdru.asdrulet5.classdata.domain.ClassDefinition;
import com.asdru.asdrulet5.classdata.domain.UltimateAbility;
import com.asdru.asdrulet5.combat.domain.Combat;
import com.asdru.asdrulet5.combat.domain.Combatant;
import com.asdru.asdrulet5.combat.exception.CombatNotFoundException;
import com.asdru.asdrulet5.combat.web.CombatMapper;
import com.asdru.asdrulet5.combat.web.dto.CombatStateDto;
import com.asdru.asdrulet5.enemydata.EnemyDefinitionRegistry;
import com.asdru.asdrulet5.enemydata.domain.EnemyDefinition;
import com.asdru.asdrulet5.party.domain.PartyMember;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CombatService {

    private static final String ENEMY_ID = "enemy-1";

    private final CombatRepository combatRepository;
    private final ClassDefinitionRegistry classDefinitionRegistry;
    private final EnemyDefinitionRegistry enemyDefinitionRegistry;
    private final SimpMessagingTemplate messagingTemplate;

    public CombatStateDto startCombat(String code, List<PartyMember> members, List<String> turnOrder) {
        List<Combatant> combatants = new ArrayList<>();
        for (String userId : turnOrder) {
            PartyMember member = members.stream()
                    .filter(candidate -> candidate.userId().equals(userId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Turn order references unknown member " + userId));
            combatants.add(toCombatant(member));
        }
        combatants.add(toEnemyCombatant());

        List<String> turnSequence = new ArrayList<>(turnOrder);
        turnSequence.add(ENEMY_ID);

        Combat combat = new Combat(code, combatants, turnSequence);
        combatRepository.save(combat);
        return broadcast(combat);
    }

    public CombatStateDto useAbility(String code, String actorId, String abilityId, String targetId) {
        Combat combat = getOrThrow(code);
        combat.useAbility(actorId, abilityId, targetId);
        return broadcast(combat);
    }

    public CombatStateDto endTurn(String code, String actorId) {
        Combat combat = getOrThrow(code);
        combat.endTurn(actorId);
        return broadcast(combat);
    }

    public CombatStateDto getState(String code) {
        return CombatMapper.toDto(getOrThrow(code));
    }

    private Combatant toCombatant(PartyMember member) {
        ClassDefinition definition = classDefinitionRegistry.get(member.characterClass());
        int ultimateChargeThreshold = definition.abilities().stream()
                .filter(UltimateAbility.class::isInstance)
                .map(UltimateAbility.class::cast)
                .findFirst()
                .orElseThrow()
                .chargeThreshold();
        return new Combatant(
                member.userId(), member.displayName(), false, member.characterClass(),
                definition.stats().maxHealth(), definition.stats().maxStamina(), definition.stats().defense(),
                definition.stats().damage(), ultimateChargeThreshold, definition.abilities(), null, null, null, null);
    }

    private Combatant toEnemyCombatant() {
        EnemyDefinition definition = enemyDefinitionRegistry.get(EnemyDefinitionRegistry.DEFAULT_ENEMY_ID);
        return new Combatant(
                ENEMY_ID, definition.displayName(), true, null,
                definition.stats().maxHealth(), definition.stats().maxStamina(), definition.stats().defense(),
                definition.stats().damage(), 0, List.of(), definition.attackName(), definition.attackDescription(),
                definition.attackEffectSummary(), definition.attackEffect());
    }

    private Combat getOrThrow(String code) {
        return combatRepository.findByCode(code).orElseThrow(() -> new CombatNotFoundException(code));
    }

    private CombatStateDto broadcast(Combat combat) {
        CombatStateDto dto = CombatMapper.toDto(combat);
        messagingTemplate.convertAndSend("/topic/party/" + combat.code() + "/combat", dto);
        return dto;
    }
}
