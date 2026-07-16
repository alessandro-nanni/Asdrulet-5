package com.asdru.asdrulet5.combat.domain;

import com.asdru.asdrulet5.classdata.domain.*;
import com.asdru.asdrulet5.combat.exception.*;
import lombok.Getter;
import lombok.Synchronized;
import lombok.experimental.Accessors;

import java.util.*;

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

    /**
     * The hit/heal events produced by the most recent {@link #useAbility} or
     * {@link #endTurn} call — replaced (not accumulated) on every such call,
     * including with an empty list when nothing happened. Lets the frontend
     * render one popup per individual hit (e.g. each strike of a multi-hit
     * ability) instead of inferring a single net change from before/after
     * health totals.
     */
    @Getter
    @Accessors(fluent = true)
    private List<CombatEvent> lastEvents = List.of();

    public Combat(String code, List<Combatant> combatants, List<String> turnSequence) {
        this.code = code;
        for (Combatant combatant : combatants) {
            this.combatants.put(combatant.combatantId(), combatant);
        }
        this.turnSequence = List.copyOf(turnSequence);
        // Attached only once every combatant exists, and to a live view of
        // the map's values — so Combatant.deadAllyCount() always reflects
        // whoever's actually died since, without needing to be re-attached.
        this.combatants.values().forEach(combatant -> combatant.attachRoster(this.combatants.values()));
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

        List<Combatant> targets = resolveTargets(actor, ability.targetType(), targetId);
        resolveTurnAction(actor, ability, targets);
        checkWinLoss();
        lastEvents = drainAllEvents();
    }

    /**
     * Spends whatever the ability costs (stamina for a basic, charge for an
     * ultimate), applies it to every resolved target, fires the resulting
     * hooks, and rolls a Twitching Talisman-style follow-up — the exact
     * sequence a player's {@link #useAbility} and an enemy's
     * {@link #resolveEnemyTurn} both need, so both funnel through here
     * instead of each hand-rolling their own version.
     */
    private void resolveTurnAction(Combatant actor, Ability ability, List<Combatant> targets) {
        actor.markActedThisTurn();
        AbilityEffect effect = resolveAbilityEffect(actor, ability);
        applyEffectToTargets(actor, effect, targets);
        // Twitching Talisman et al.: a basic ability (never an ultimate) has
        // a chance to immediately resolve again against the same targets,
        // free of a second cost.
        if (ability instanceof BasicAbility && triggersFollowUp(actor)) {
            applyEffectToTargets(actor, effect, targets);
        }
    }

    private AbilityEffect resolveAbilityEffect(Combatant actor, Ability ability) {
        return switch (ability) {
            case BasicAbility basic -> {
                if (!actor.hasStamina(basic.staminaCost())) {
                    throw new InsufficientResourceException(
                            actor.combatantId() + " does not have enough stamina for " + basic.name());
                }
                actor.spendStamina(basic.staminaCost());
                yield basic.effect();
            }
            case UltimateAbility ultimate -> {
                if (!actor.ultimateReady()) {
                    throw new InsufficientResourceException(actor.combatantId() + "'s ultimate is not charged yet");
                }
                actor.resetUltimateCharge();
                yield ultimate.effect();
            }
        };
    }

    private void applyEffectToTargets(Combatant actor, AbilityEffect effect, List<Combatant> targets) {
        for (Combatant target : targets) {
            int healthBefore = target.currentHealth();
            effect.apply(actor, target);
            resolveDamageHooks(actor, target, healthBefore);
        }
        effect.applyToTeam(actor, aliveAllies(actor));
    }

    private boolean triggersFollowUp(Combatant actor) {
        return actor.passives().stream().anyMatch(CombatantPassive::triggersFollowUpAbility);
    }

    /**
     * actor's own living side, actor included — see {@link AbilityEffect#applyToTeam}.
     */
    private List<EffectTarget> aliveAllies(Combatant actor) {
        return combatants.values().stream()
                .filter(c -> c.enemy() == actor.enemy() && c.alive())
                .map(EffectTarget.class::cast)
                .toList();
    }

    /**
     * Fires the damage/kill/death passive hooks for one actor-on-target
     * application, inferred from the target's health delta rather than
     * threaded through AbilityEffect itself — AbilityEffect operates on the
     * bare EffectTarget abstraction and has no notion of equipped items, so
     * this is the narrowest point that both knows "damage happened" and has
     * the actual Combatants (and their passives) in hand. A no-op for heals
     * (health only ever goes up), so this only ever fires for real damage.
     * The critical flag comes from {@code target.lastDamageTaken()} rather
     * than being recomputed here — for a multi-hit ability this is whichever
     * hit landed last, since the health delta itself is already a total
     * across every hit in the application, not a single one.
     */
    private void resolveDamageHooks(Combatant actor, Combatant target, int targetHealthBefore) {
        int damageDealt = targetHealthBefore - target.currentHealth();
        if (damageDealt <= 0) {
            return;
        }
        Damage damage = new Damage(damageDealt, target.lastDamageTaken().critical());
        actor.passives().forEach(passive -> passive.onDamageDealt(actor, target, damage));
        target.passives().forEach(passive -> passive.onDamageTaken(target, actor, damage));
        // activeEffects() is already a defensive copy (see Combatant), so
        // this is safe even though the reflected hit below can itself
        // trigger further mutation of the real underlying list.
        target.activeEffects().forEach(effect -> effect.onDamageTaken(target, actor, damage));
        if (!target.alive()) {
            actor.passives().forEach(passive -> passive.onKill(actor, target));
            target.passives().forEach(passive -> passive.onDeath(target));
        }
    }

    @Synchronized
    public void endTurn(String actorId) {
        requireInProgress();
        requireCurrentTurn(actorId);
        Combatant actor = requireCombatant(actorId);
        actor.passives().forEach(passive -> passive.onEndTurn(actor));
        advanceTurn();
        lastEvents = drainAllEvents();
    }

    private List<CombatEvent> drainAllEvents() {
        List<CombatEvent> drained = new ArrayList<>();
        for (Combatant combatant : combatants.values()) {
            drained.addAll(combatant.drainEvents());
        }
        return List.copyOf(drained);
    }

    private List<Combatant> resolveTargets(Combatant actor, TargetType targetType, String targetId) {
        return switch (targetType) {
            case SELF -> List.of(actor);
            case SINGLE_ALLY -> List.of(requireAliveTarget(targetId, actor.enemy()));
            // Area effects still hit every enemy regardless of Taunt (see
            // ActiveEffect.forcedTargetId's own doc) — only a single-enemy
            // pick has to be validated against it.
            case SINGLE_ENEMY ->
                    List.of(requireAliveTarget(requireUntauntedOrForcedTarget(actor, targetId), !actor.enemy()));
            case ALL_ALLIES -> combatants.values().stream()
                    .filter(c -> c.enemy() == actor.enemy() && c.alive())
                    .toList();
            case ALL_ENEMIES -> combatants.values().stream()
                    .filter(c -> c.enemy() != actor.enemy() && c.alive())
                    .toList();
        };
    }

    /**
     * Taunted actors may only choose the combatant that taunted them as a SINGLE_ENEMY target — see Taunt.
     */
    private String requireUntauntedOrForcedTarget(Combatant actor, String targetId) {
        String forcedId = forcedTargetId(actor);
        if (forcedId != null && !forcedId.equals(targetId)) {
            throw new InvalidTargetException(actor.combatantId() + " is taunted and must target " + forcedId);
        }
        return targetId;
    }

    private String forcedTargetId(Combatant actor) {
        return actor.activeEffects().stream()
                .map(ActiveEffect::forcedTargetId)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private Combatant requireAliveTarget(String targetId, boolean expectedEnemyFlag) {
        Combatant target = combatants.get(targetId);
        if (target == null || target.enemy() != expectedEnemyFlag || !target.alive()) {
            throw new InvalidTargetException("Invalid target: " + targetId);
        }
        return target;
    }

    /**
     * Picks the lowest-health living ally as usual, unless enemyActor is
     * taunted — then whoever taunted them is the only valid choice, as long
     * as that taunter is still alive (a dead taunter can no longer be
     * attacked, so the normal lowest-health pick takes back over).
     */
    private void resolveEnemyTurn(Combatant enemyActor) {
        Combatant target = tauntedTarget(enemyActor)
                .filter(Combatant::alive)
                .orElseGet(() -> combatants.values().stream()
                        .filter(c -> !c.enemy() && c.alive())
                        .min(Comparator.comparingInt(Combatant::currentHealth).thenComparing(Combatant::combatantId))
                        .orElse(null));
        if (target == null) {
            return;
        }
        // Same cost-spend-and-fire-hooks path a player's useAbility goes
        // through (see resolveTurnAction) — an enemy's turn is really just
        // "use one of my own abilities," no different in kind from a
        // player's, so there's no reason for this to be a separate,
        // hand-rolled sequence. Always its first ability for now — see
        // EnemyDefinitionRegistry's own doc on enemies eventually having more
        // than one and needing real move selection.
        Ability ability = enemyActor.abilities().getFirst();
        resolveTurnAction(enemyActor, ability, List.of(target));
        checkWinLoss();
    }

    private Optional<Combatant> tauntedTarget(Combatant actor) {
        String forcedId = forcedTargetId(actor);
        return forcedId == null ? Optional.empty() : Optional.ofNullable(combatants.get(forcedId));
    }

    private void advanceTurn() {
        for (int i = 0; i < turnSequence.size() * 2; i++) {
            currentTurnIndex = (currentTurnIndex + 1) % turnSequence.size();
            Combatant next = combatants.get(turnSequence.get(currentTurnIndex));
            if (!next.alive()) {
                continue;
            }
            // Checked before ticking: an effect due to expire on exactly this
            // turn (e.g. the final turn of a 2-turn Frozen) must still
            // prevent this turn's action — tickActiveEffects() below would
            // otherwise remove it first and this combatant would wrongly act.
            boolean prevented = isPrevented(next);
            next.resetActedThisTurn();
            next.tickActiveEffects();
            next.passives().forEach(passive -> passive.onStartTurn(next));
            if (prevented) {
                // Frozen (or similar): still ticks down on this combatant's
                // own turn, but neither a human player nor the enemy AI gets
                // to act — move straight on to whoever's next.
                continue;
            }
            if (!next.enemy()) {
                next.restoreStamina(STAMINA_REGEN_PER_TURN);
                return;
            }
            resolveEnemyTurn(next);
            if (status != CombatStatus.IN_PROGRESS) {
                return;
            }
        }
        throw new IllegalStateException("Unable to find next combatant to act");
    }

    private boolean isPrevented(Combatant combatant) {
        return combatant.activeEffects().stream().anyMatch(ActiveEffect::preventsAction);
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
