package com.asdru.asdrulet5.combat.domain;

import com.asdru.asdrulet5.classdata.domain.Ability;
import com.asdru.asdrulet5.classdata.domain.AbilityEffect;
import com.asdru.asdrulet5.classdata.domain.ActiveEffect;
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
    private final int maxStamina;
    private final int baseDefense;
    private final int ultimateChargeThreshold;
    private final List<ActiveEffect> activeEffects = new ArrayList<>();
    private final List<Ability> abilities;
    private final String attackName;
    private final String attackDescription;
    private final String attackEffectSummary;
    private final AbilityEffect attackEffect;
    private int currentHealth;
    private int currentStamina;
    private int ultimateCharge;

    public Combatant(String id, String displayName, boolean enemy, CharacterClass characterClass,
                     int maxHealth, int maxStamina, int baseDefense, int ultimateChargeThreshold,
                     List<Ability> abilities, String attackName, String attackDescription,
                     String attackEffectSummary, AbilityEffect attackEffect) {
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
        this.attackEffectSummary = attackEffectSummary;
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
        return baseDefense + activeEffects.stream().mapToInt(ActiveEffect::defenseBonus).sum();
    }

    @Override
    public int bonusDamage() {
        return activeEffects.stream().mapToInt(ActiveEffect::damageBonus).sum();
    }

    public boolean hasStamina(int amount) {
        return currentStamina >= amount;
    }

    public boolean ultimateReady() {
        return ultimateCharge >= ultimateChargeThreshold;
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
    public void addActiveEffect(ActiveEffect effect) {
        // Reapplying an effect with the same name (see ActiveEffect's
        // identity contract) refreshes it in place rather than stacking a
        // duplicate copy.
        activeEffects.removeIf(existing -> existing.name().equals(effect.name()));
        activeEffects.add(effect);
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
