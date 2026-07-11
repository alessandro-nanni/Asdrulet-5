package com.asdru.asdrulet5.combat.domain;

import com.asdru.asdrulet5.classdata.domain.Ability;
import com.asdru.asdrulet5.classdata.domain.AbilityEffect;
import com.asdru.asdrulet5.classdata.domain.BuffKind;
import com.asdru.asdrulet5.classdata.domain.EffectTarget;
import com.asdru.asdrulet5.party.domain.CharacterClass;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

/**
 * Mutable, unlike most domain objects in this codebase (e.g. PartyMember):
 * health/stamina/charge/buffs all change on nearly every combat action, so an
 * immutable-record-plus-withers approach would mean an 10+ argument
 * constructor call on every mutation.
 *
 * <p>Implements {@link EffectTarget} so ability effects (classdata.domain)
 * can apply themselves directly to a Combatant without classdata depending
 * on the combat package — the mutators below are public only because Java
 * requires interface implementations to be; callers other than
 * {@link Combat} and applied {@link AbilityEffect}s still shouldn't invoke
 * them directly.
 */
@Getter
@Accessors(fluent = true)
public final class Combatant implements EffectTarget {

    private final String id;
    private final String displayName;
    private final boolean enemy;
    private final CharacterClass characterClass;
    private final int maxHealth;
    private int currentHealth;
    private final int maxStamina;
    private int currentStamina;
    private final int baseDefense;
    private int ultimateCharge;
    private final int ultimateChargeThreshold;
    private final List<ActiveEffect> activeEffects = new ArrayList<>();
    private final List<Ability> abilities;
    private final String attackName;
    private final String attackDescription;
    private final AbilityEffect attackEffect;

    public Combatant(String id, String displayName, boolean enemy, CharacterClass characterClass,
                      int maxHealth, int maxStamina, int baseDefense, int ultimateChargeThreshold,
                      List<Ability> abilities, String attackName, String attackDescription,
                      AbilityEffect attackEffect) {
        this.id = id;
        this.displayName = displayName;
        this.enemy = enemy;
        this.characterClass = characterClass;
        this.maxHealth = maxHealth;
        this.currentHealth = maxHealth;
        this.maxStamina = maxStamina;
        this.currentStamina = maxStamina;
        this.baseDefense = baseDefense;
        this.ultimateCharge = 0;
        this.ultimateChargeThreshold = ultimateChargeThreshold;
        this.abilities = List.copyOf(abilities);
        this.attackName = attackName;
        this.attackDescription = attackDescription;
        this.attackEffect = attackEffect;
    }

    public boolean alive() {
        return currentHealth > 0;
    }

    public List<ActiveEffect> activeEffects() {
        return List.copyOf(activeEffects);
    }

    @Override
    public int effectiveDefense() {
        return baseDefense + sumActive(BuffKind.DEFENSE);
    }

    @Override
    public int bonusDamage() {
        return sumActive(BuffKind.DAMAGE);
    }

    public boolean hasStamina(int amount) {
        return currentStamina >= amount;
    }

    public boolean ultimateReady() {
        return ultimateCharge >= ultimateChargeThreshold;
    }

    private int sumActive(BuffKind kind) {
        return activeEffects.stream().filter(effect -> effect.kind() == kind).mapToInt(ActiveEffect::power).sum();
    }

    @Override
    public void applyDamage(int amount) {
        currentHealth = Math.max(0, currentHealth - amount);
    }

    @Override
    public void applyHeal(int amount) {
        currentHealth = Math.min(maxHealth, currentHealth + amount);
    }

    @Override
    public void addActiveEffect(BuffKind kind, int power, int durationTurns) {
        activeEffects.add(new ActiveEffect(kind, power, durationTurns));
    }

    @Override
    public void addUltimateCharge(int amount) {
        ultimateCharge = Math.min(ultimateChargeThreshold, ultimateCharge + amount);
    }

    void spendStamina(int amount) {
        currentStamina = Math.max(0, currentStamina - amount);
    }

    void regenerateStamina(int amount) {
        currentStamina = Math.min(maxStamina, currentStamina + amount);
    }

    void resetUltimateCharge() {
        ultimateCharge = 0;
    }

    void tickActiveEffects() {
        // Filter before decrementing: ActiveEffect requires remainingTurns > 0,
        // so an effect on its last turn (remainingTurns == 1) is dropped here
        // rather than ticked down to an invalid 0.
        List<ActiveEffect> ticked = activeEffects.stream()
                .filter(effect -> effect.remainingTurns() > 1)
                .map(ActiveEffect::withTurnElapsed)
                .toList();
        activeEffects.clear();
        activeEffects.addAll(ticked);
    }
}
