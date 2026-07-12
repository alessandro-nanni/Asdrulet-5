package com.asdru.asdrulet5.classdata.domain;

import static com.asdru.asdrulet5.classdata.domain.Preconditions.requireNonBlank;

/**
 * A temporary effect attached to a combatant for a limited number of turns.
 * Not restricted to a closed set of kinds — define a new effect by
 * subclassing (directly, or via the static factories below) and overriding
 * whatever behavior applies:
 *
 * <ul>
 *     <li>{@link #onTick} for effects that act each turn — damage/heal over
 *     time, and anything else that needs to touch the holder turn by turn.
 *     Called once per turn the effect is active, before its duration
 *     decrements.</li>
 *     <li>{@link #defenseBonus()}/{@link #damageBonus()} for passive stat
 *     modifiers that apply for as long as the effect remains attached —
 *     read live by {@code EffectTarget.effectiveDefense()}/{@code
 *     bonusDamage()} each time they're queried, so there's nothing to revert
 *     when the effect expires.</li>
 * </ul>
 *
 * <p>{@code name}/{@code description} are author-facing display text (shown
 * in the UI), not behavior — same role as {@link Ability#effectSummary()}.
 * {@code name} doubles as this effect's identity: reapplying an effect with
 * the same name onto a holder that already has one replaces it (fresh
 * duration and power) instead of stacking a duplicate — see {@code
 * EffectTarget.addActiveEffect}. Give distinct abilities distinct names even
 * if they modify the same stat (e.g. "Taunt" vs "Fortress Stance"), so they
 * can coexist instead of overwriting each other.
 *
 * <p>{@code icon} is a stable key (e.g. "shield", "poison") that the
 * frontend maps to an actual icon to render — not a literal file path, since
 * the client renders icons as themeable inline SVG components rather than
 * standalone assets. An unrecognized key falls back to a generic icon
 * client-side, so introducing a new effect never requires a backend enum
 * change — just picking a key and, if it's new, adding it to the frontend's
 * lookup.
 */
public abstract class ActiveEffect {

    private final String name;
    private final String description;
    private final String icon;
    private int remainingTurns;

    protected ActiveEffect(String name, String description, String icon, int remainingTurns) {
        requireNonBlank(name, "name");
        requireNonBlank(description, "description");
        requireNonBlank(icon, "icon");
        if (remainingTurns <= 0) {
            throw new IllegalArgumentException("remainingTurns must be positive");
        }
        this.name = name;
        this.description = description;
        this.icon = icon;
        this.remainingTurns = remainingTurns;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public String icon() {
        return icon;
    }

    public int remainingTurns() {
        return remainingTurns;
    }

    /**
     * Runs this effect's per-turn behavior against its holder. No-op by
     * default — override for damage/heal-over-time or similar.
     */
    public void onTick(EffectTarget holder) {
    }

    /**
     * Passive defense contribution while this effect is active. 0 by
     * default — override for a defense buff/debuff.
     */
    public int defenseBonus() {
        return 0;
    }

    /**
     * Passive damage contribution while this effect is active. 0 by
     * default — override for a damage buff/debuff.
     */
    public int damageBonus() {
        return 0;
    }

    /**
     * Ticks this effect against its holder and counts down its duration.
     * Called once per turn by the combat turn engine, which owns removing
     * expired effects. Not for use outside that engine.
     */
    public final boolean tick(EffectTarget holder) {
        onTick(holder);
        remainingTurns--;
        return remainingTurns <= 0;
    }

    public static ActiveEffect defenseBuff(String name, String icon, int power, int durationTurns) {
        requirePositive(power, "power");
        return new ActiveEffect(name, statChangeDescription("defense", power, durationTurns), icon, durationTurns) {
            @Override
            public int defenseBonus() {
                return power;
            }
        };
    }

    public static ActiveEffect damageBuff(String name, String icon, int power, int durationTurns) {
        requirePositive(power, "power");
        return new ActiveEffect(name, statChangeDescription("damage", power, durationTurns), icon, durationTurns) {
            @Override
            public int damageBonus() {
                return power;
            }
        };
    }

    public static ActiveEffect damageOverTime(String name, String description, String icon, int powerPerTurn, int durationTurns) {
        requirePositive(powerPerTurn, "powerPerTurn");
        return new ActiveEffect(name, description, icon, durationTurns) {
            @Override
            public void onTick(EffectTarget holder) {
                holder.applyDamage(powerPerTurn);
            }
        };
    }

    public static ActiveEffect healOverTime(String name, String description, String icon, int powerPerTurn, int durationTurns) {
        requirePositive(powerPerTurn, "powerPerTurn");
        return new ActiveEffect(name, description, icon, durationTurns) {
            @Override
            public void onTick(EffectTarget holder) {
                holder.applyHeal(powerPerTurn);
            }
        };
    }

    private static String statChangeDescription(String stat, int power, int durationTurns) {
        String turns = durationTurns == 1 ? "1 turn" : durationTurns + " turns";
        return "+" + power + " " + stat + " for " + turns + ".";
    }

    private static void requirePositive(int value, String field) {
        if (value <= 0) {
            throw new IllegalArgumentException(field + " must be positive");
        }
    }
}
