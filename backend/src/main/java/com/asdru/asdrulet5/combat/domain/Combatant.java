package com.asdru.asdrulet5.combat.domain;

import com.asdru.asdrulet5.classdata.domain.*;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Shared state and behavior for anyone in a fight — see {@link PlayerCombatant}
 * and {@link EnemyCombatant} for the two things a Combatant actually is.
 * Everything here applies equally to both sides: stats, health/stamina/charge,
 * active effects, abilities, and passives. What's deliberately *not* here —
 * {@code characterClass} (players only) and each side's own notion of "which
 * one am I" ({@code PlayerCombatant} just reuses the party member's own id;
 * {@code EnemyCombatant} additionally tracks which {@code EnemyDefinition} it
 * came from) — lives on the concrete subclasses instead, since a raw
 * {@code boolean enemy} field passed into one shared constructor was exactly
 * the kind of thing that let player-only and enemy-only concerns leak into
 * each other.
 *
 * <p>Mutable, unlike most domain objects in this codebase (e.g. PartyMember):
 * health/stamina/charge/buffs all change on nearly every combat action, so an
 * immutable-record-plus-withers approach would mean a 10+ argument
 * constructor call on every mutation.
 *
 * <p>Implements {@link EffectTarget} so ability effects (classdata.domain)
 * can apply themselves directly to a Combatant without classdata depending
 * on the combat package — the mutators below are public only because Java
 * requires interface implementations to be; callers other than
 * {@link Combat} and applied ability effects still shouldn't invoke them
 * directly.
 */
@Getter
@Accessors(fluent = true)
public abstract class Combatant implements EffectTarget {

    private final String combatantId;
    private final String displayName;
    private final Stats stats;
    private final int ultimateChargeThreshold;
    private final List<ActiveEffect> activeEffects = new ArrayList<>();
    private final List<CombatEvent> events = new ArrayList<>();
    private final List<Ability> abilities;
    private final List<CombatantPassive> passives;
    private int currentHealth;
    private int currentStamina;
    private int ultimateCharge;
    /**
     * Whether this combatant has already resolved an action (an ability or an
     * enemy's auto-attack — see {@code Combat.resolveTurnAction}) since their
     * current turn began. Reset by {@code Combat.advanceTurn} the moment a
     * turn actually starts for them. Broadcast to every client so the
     * lean-forward "engaged" visual reflects whoever's really acting for the
     * whole party, not just whichever client happened to trigger it.
     */
    private boolean actedThisTurn = false;
    /**
     * Every combatant in this fight (both sides), attached by {@link Combat}'s constructor once the whole roster exists — see {@link #deadAllyCount()}. Empty (not null) until then, so a bare Combatant built outside a Combat just reports zero dead allies.
     */
    private Collection<Combatant> roster = List.of();
    /**
     * The most recent {@link Damage} this combatant took — read by {@code Combat.resolveDamageHooks} to recover whether the hit that just landed was critical, since that method otherwise only sees the net health delta. Starts as a harmless zero, non-critical placeholder.
     */
    private Damage lastDamageTaken = Damage.of(0);

    protected Combatant(String combatantId, String displayName, Stats stats, int ultimateChargeThreshold,
                        List<Ability> abilities, List<? extends CombatantPassive> passives) {
        this.combatantId = combatantId;
        this.displayName = displayName;
        this.stats = stats;
        this.currentHealth = stats.maxHealth();
        this.currentStamina = stats.maxStamina();
        this.ultimateCharge = 0;
        this.ultimateChargeThreshold = ultimateChargeThreshold;
        this.abilities = List.copyOf(abilities);
        this.passives = List.copyOf(passives);
    }

    /**
     * Owned entirely by the concrete subclass — see this class's own doc.
     */
    public abstract boolean enemy();

    public boolean alive() {
        return currentHealth > 0;
    }

    @Override
    public int maxHealth() {
        return stats.maxHealth();
    }

    public int maxStamina() {
        return stats.maxStamina();
    }

    /**
     * Overrides the health this combatant starts the fight at (e.g. carried
     * over from a MYSTERY wheel spin), clamped to this combatant's own max —
     * a wheel roll is computed against the member's own effective max health,
     * which can differ slightly from this fresh combatant's if gear changed
     * since. Raw, unlike {@link #applyDamage}/{@link #applyHeal}: no event is
     * recorded, since nothing has actually happened in this fight yet.
     */
    public void setStartingHealth(int health) {
        this.currentHealth = Math.max(0, Math.min(stats.maxHealth(), health));
    }

    public List<ActiveEffect> activeEffects() {
        return List.copyOf(activeEffects);
    }

    @Override
    public int effectiveDefense() {
        return stats.defense() + activeEffects.stream().mapToInt(ActiveEffect::defenseBonus).sum();
    }

    @Override
    public int bonusDamage() {
        return activeEffects.stream().mapToInt(ActiveEffect::damageBonus).sum();
    }

    /**
     * Summed fresh from {@link #passives} on every call rather than
     * precomputed once at construction — a passive's own contribution can
     * depend on live combat state (Scythe) or randomness (Lucky Charm), and
     * even the flat {@link CombatantPassive#damagePercent()} case is cheap
     * enough to just re-sum. Subclasses that need to add a fight-start-only,
     * externally-computed extra (see PlayerCombatant's party-leader-relative
     * bonus) do so by overriding and adding to {@code super.damagePercentBonus()}.
     */
    @Override
    public int damagePercentBonus() {
        return passives.stream().mapToInt(CombatantPassive::damagePercent).sum()
                + activeEffects.stream().mapToInt(ActiveEffect::damagePercentBonus).sum()
                + passives.stream().mapToInt(passive -> passive.damagePercentBonus(this)).sum();
    }

    /**
     * Called once by {@link Combat}'s constructor, after every combatant in the fight has been created — see {@link #roster}.
     */
    void attachRoster(Collection<Combatant> roster) {
        this.roster = roster;
    }

    @Override
    public int deadAllyCount() {
        return (int) roster.stream()
                .filter(other -> other != this && other.enemy() == this.enemy() && !other.alive())
                .count();
    }

    public boolean hasStamina(int amount) {
        return currentStamina >= amount;
    }

    public boolean ultimateReady() {
        return ultimateCharge >= ultimateChargeThreshold;
    }

    @Override
    public void applyDamage(Damage damage) {
        int before = currentHealth;
        currentHealth = Math.max(0, currentHealth - damage.amount());
        lastDamageTaken = damage;
        recordEvent(CombatEvent.Kind.DAMAGE, before - currentHealth, damage.critical());
    }

    @Override
    public void applyHeal(int amount) {
        int before = currentHealth;
        currentHealth = Math.min(stats.maxHealth(), currentHealth + amount);
        recordEvent(CombatEvent.Kind.HEAL, currentHealth - before, false);
    }

    /**
     * Records the actual health change (post-clamp, e.g. an overkill hit or
     * an overheal only records however much health really moved) so the
     * event matches what shows on the health bar rather than the effect's
     * raw input. Zero-change applications (already dead, already full) are
     * dropped rather than recorded as no-op events.
     */
    private void recordEvent(CombatEvent.Kind kind, int actualAmount, boolean critical) {
        if (actualAmount > 0) {
            events.add(new CombatEvent(combatantId, kind, actualAmount, critical));
        }
    }

    @Override
    public void addActiveEffect(ActiveEffect effect) {
        // Reapplying an effect with the same name (see ActiveEffect's
        // identity contract) refreshes it in place rather than stacking a
        // duplicate copy.
        activeEffects.removeIf(existing -> existing.name().equals(effect.name()));
        activeEffects.add(effect);
    }

    @Override
    public void clearNegativeActiveEffects() {
        activeEffects.removeIf(ActiveEffect::isNegative);
    }

    @Override
    public void addUltimateCharge(int amount) {
        ultimateCharge = Math.min(ultimateChargeThreshold, ultimateCharge + amount);
    }

    void spendStamina(int amount) {
        currentStamina = Math.max(0, currentStamina - amount);
    }

    @Override
    public void restoreStamina(int amount) {
        currentStamina = Math.min(stats.maxStamina(), currentStamina + amount);
    }

    @Override
    public void drainStamina(int amount) {
        currentStamina = Math.max(0, currentStamina - amount);
    }

    void resetUltimateCharge() {
        ultimateCharge = 0;
    }

    void markActedThisTurn() {
        actedThisTurn = true;
    }

    void resetActedThisTurn() {
        actedThisTurn = false;
    }

    /**
     * Returns and clears the events recorded since the last drain.
     */
    List<CombatEvent> drainEvents() {
        List<CombatEvent> drained = List.copyOf(events);
        events.clear();
        return drained;
    }

    void tickActiveEffects() {
        List<ActiveEffect> stillActive = new ArrayList<>();
        for (ActiveEffect effect : activeEffects) {
            boolean expired = effect.tick(this);
            if (!expired) {
                stillActive.add(effect);
            }
        }
        activeEffects.clear();
        activeEffects.addAll(stillActive);
    }
}
