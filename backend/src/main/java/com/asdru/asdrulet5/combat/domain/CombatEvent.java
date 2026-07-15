package com.asdru.asdrulet5.combat.domain;

/**
 * A single hit or heal applied to a combatant during the most recent action
 * — one entry per individual application, not a net total. A 4-hit ability
 * produces 4 events against the same target, letting the frontend render a
 * popup per hit instead of collapsing them into one combined number.
 * {@code critical} is always false for HEAL events — only a DAMAGE event can
 * ever have come from a critical hit (see {@code Damage}).
 */
public record CombatEvent(String targetId, Kind kind, int amount, boolean critical) {
    public enum Kind {DAMAGE, HEAL}
}
