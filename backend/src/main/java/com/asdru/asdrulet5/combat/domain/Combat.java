package com.asdru.asdrulet5.combat.domain;

import com.asdru.asdrulet5.classdata.domain.*;
import com.asdru.asdrulet5.combat.exception.*;
import lombok.Getter;
import lombok.Synchronized;
import lombok.experimental.Accessors;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Combat {

    /**
     * Flat stamina regained at the start of each of a combatant's own turns
     * (capped at their max) — a deliberately partial refill, not a full
     * reset, so skipping a turn to bank stamina for a bigger combo later is
     * a real tactical choice.
     */
    static final int STAMINA_REGEN_PER_TURN = 40;

    @Getter
    @Accessors(fluent = true)
    private final String code;

    private final Map<String, Combatant> combatants = new LinkedHashMap<>();
    private final List<String> turnSequence;
    private int currentTurnIndex = 0;

    @Getter
    @Accessors(fluent = true)
    private CombatStatus status = CombatStatus.IN_PROGRESS;

    public Combat(String code, List<Combatant> combatants, List<String> turnSequence) {
        this.code = code;
        for (Combatant combatant : combatants) {
            this.combatants.put(combatant.id(), combatant);
        }
        this.turnSequence = List.copyOf(turnSequence);
    }

    @Synchronized
    public List<Combatant> combatants() {
        return List.copyOf(combatants.values());
    }

    @Synchronized
    public String currentTurnCombatantId() {
        return turnSequence.get(currentTurnIndex);
    }

    @Synchronized
    public void useAbility(String actorId, String abilityId, String targetId) {
        requireInProgress();
        requireCurrentTurn(actorId);
        Combatant actor = requireCombatant(actorId);
        Ability ability = actor.abilities().stream()
                .filter(candidate -> candidate.id().equals(abilityId))
                .findFirst()
                .orElseThrow(() -> new UnknownAbilityException(actorId, abilityId));

        AbilityEffect effect = switch (ability) {
            case BasicAbility basic -> {
                if (!actor.hasStamina(basic.staminaCost())) {
                    throw new InsufficientResourceException(
                            actorId + " does not have enough stamina for " + basic.name());
                }
                actor.spendStamina(basic.staminaCost());
                yield basic.effect();
            }
            case UltimateAbility ultimate -> {
                if (!actor.ultimateReady()) {
                    throw new InsufficientResourceException(actorId + "'s ultimate is not charged yet");
                }
                actor.resetUltimateCharge();
                yield ultimate.effect();
            }
        };

        List<Combatant> targets = resolveTargets(actor, ability.targetType(), targetId);
        for (Combatant target : targets) {
            effect.apply(actor, target);
        }
        checkWinLoss();
    }

    @Synchronized
    public void endTurn(String actorId) {
        requireInProgress();
        requireCurrentTurn(actorId);
        advanceTurn();
    }

    private List<Combatant> resolveTargets(Combatant actor, TargetType targetType, String targetId) {
        return switch (targetType) {
            case SELF -> List.of(actor);
            case SINGLE_ALLY -> List.of(requireAliveTarget(targetId, actor.enemy()));
            case SINGLE_ENEMY -> List.of(requireAliveTarget(targetId, !actor.enemy()));
            case ALL_ALLIES -> combatants.values().stream()
                    .filter(c -> c.enemy() == actor.enemy() && c.alive())
                    .toList();
            case ALL_ENEMIES -> combatants.values().stream()
                    .filter(c -> c.enemy() != actor.enemy() && c.alive())
                    .toList();
        };
    }

    private Combatant requireAliveTarget(String targetId, boolean expectedEnemyFlag) {
        Combatant target = combatants.get(targetId);
        if (target == null || target.enemy() != expectedEnemyFlag || !target.alive()) {
            throw new InvalidTargetException("Invalid target: " + targetId);
        }
        return target;
    }

    private void resolveEnemyTurn(Combatant enemyActor) {
        Combatant target = combatants.values().stream()
                .filter(c -> !c.enemy() && c.alive())
                .min(Comparator.comparingInt(Combatant::currentHealth).thenComparing(Combatant::id))
                .orElse(null);
        if (target == null) {
            return;
        }
        enemyActor.attackEffect().apply(enemyActor, target);
        checkWinLoss();
    }

    private void advanceTurn() {
        for (int i = 0; i < turnSequence.size() * 2; i++) {
            currentTurnIndex = (currentTurnIndex + 1) % turnSequence.size();
            Combatant next = combatants.get(turnSequence.get(currentTurnIndex));
            if (!next.alive()) {
                continue;
            }
            next.tickActiveEffects();
            if (!next.enemy()) {
                next.regenerateStamina(STAMINA_REGEN_PER_TURN);
                return;
            }
            resolveEnemyTurn(next);
            if (status != CombatStatus.IN_PROGRESS) {
                return;
            }
        }
        throw new IllegalStateException("Unable to find next combatant to act");
    }

    private void checkWinLoss() {
        if (status != CombatStatus.IN_PROGRESS) {
            return;
        }
        boolean allEnemiesDead = combatants.values().stream().filter(Combatant::enemy).noneMatch(Combatant::alive);
        boolean allPlayersDead = combatants.values().stream().filter(c -> !c.enemy()).noneMatch(Combatant::alive);
        if (allEnemiesDead) {
            status = CombatStatus.PARTY_WON;
        } else if (allPlayersDead) {
            status = CombatStatus.PARTY_LOST;
        }
    }

    private void requireInProgress() {
        if (status != CombatStatus.IN_PROGRESS) {
            throw new CombatNotInProgressException(code);
        }
    }

    private void requireCurrentTurn(String actorId) {
        if (!currentTurnCombatantId().equals(actorId)) {
            throw new NotYourTurnException(actorId);
        }
    }

    private Combatant requireCombatant(String id) {
        Combatant combatant = combatants.get(id);
        if (combatant == null) {
            throw new InvalidTargetException("Unknown combatant: " + id);
        }
        return combatant;
    }
}
