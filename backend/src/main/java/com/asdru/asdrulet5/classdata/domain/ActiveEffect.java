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
                holder.applyDamage(Damage.of(powerPerTurn));
            }

            @Override
            public boolean isNegative() {
                return true;
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

    /**
     * Cannot act on their own turn at all — see {@link #preventsAction()}. Neither buffs nor damages on its own.
     */
    public static ActiveEffect frozen(String name, String icon, int durationTurns) {
        return new ActiveEffect(name, "Frozen solid — cannot act for " + turnsLabel(durationTurns) + ".", icon, durationTurns) {
            @Override
            public boolean preventsAction() {
                return true;
            }

            @Override
            public boolean isNegative() {
                return true;
            }
        };
    }

    /**
     * Percentage damage buff — e.g. power 5 means +5% damage.
     */
    public static ActiveEffect strength(String name, String icon, int power, int durationTurns) {
        requirePositive(power, "power");
        return new ActiveEffect(name, "+" + power + "% damage for " + turnsLabel(durationTurns) + ".", icon, durationTurns) {
            @Override
            public int damagePercentBonus() {
                return power;
            }
        };
    }

    /**
     * Restricts the holder's own SINGLE_ENEMY ability choices to whoever
     * applied this — see {@link #forcedTargetId()}. tauntedById is the
     * taunter's combatant id (typically the caster's own {@code actor.combatantId()}
     * at the moment this is applied) — that's what target validation actually
     * checks against. tauntedByDisplayName is the same taunter's player- or
     * author-facing name, used only for the description text below; kept
     * separate from tauntedById since the id (e.g. "enemy-2") isn't
     * meaningful to show a player.
     */
    public static ActiveEffect taunt(String name, String icon, int durationTurns, String tauntedById, String tauntedByDisplayName) {
        requireNonBlank(tauntedById, "tauntedById");
        requireNonBlank(tauntedByDisplayName, "tauntedByDisplayName");
        return new ActiveEffect(name, "Can only target " + tauntedByDisplayName + " for " + turnsLabel(durationTurns) + ".", icon, durationTurns) {
            @Override
            public String forcedTargetId() {
                return tauntedById;
            }

            @Override
            public boolean isNegative() {
                return true;
            }
        };
    }

    /**
     * 10% of damage taken is dealt back to whoever dealt it.
     */
    public static ActiveEffect thorns(String name, String icon, int durationTurns) {
        return new ActiveEffect(name, "Reflects 10% of damage taken back at the attacker for "
                + turnsLabel(durationTurns) + ".", icon, durationTurns) {
            @Override
            public void onDamageTaken(EffectTarget holder, EffectTarget attacker, Damage damage) {
                int healthBefore = attacker.currentHealth();
                // A reflected hit isn't itself a fresh crit roll.
                attacker.applyDamage(Damage.of(Math.max(1, damage.amount() / 10)));
                // holder is the one reflecting here, not attacker — this bypasses
                // the ability-resolution path combat.domain.Combat's own damage
                // bookkeeping watches, so it has to record itself.
                int actualDamage = healthBefore - attacker.currentHealth();
                if (actualDamage > 0) {
                    holder.recordDamageDealt(actualDamage);
                }
            }
        };
    }

    /**
     * Whoever lands a hit on the holder heals for 10% of the damage they just dealt.
     */
    public static ActiveEffect goldenTouch(String name, String icon, int durationTurns) {
        return new ActiveEffect(name, "Whoever hits this target heals for 10% of the damage dealt, for "
                + turnsLabel(durationTurns) + ".", icon, durationTurns) {
            @Override
            public void onDamageTaken(EffectTarget holder, EffectTarget attacker, Damage damage) {
                int healthBefore = attacker.currentHealth();
                attacker.applyHeal(Math.max(1, damage.amount() / 10));
                // attacker heals themselves here — this bypasses the
                // ability-resolution path combat.domain.Combat's own healing
                // bookkeeping watches, so it has to record itself.
                int actualHeal = attacker.currentHealth() - healthBefore;
                if (actualHeal > 0) {
                    attacker.recordHealingDone(actualHeal);
                }
            }

            @Override
            public boolean isNegative() {
                return true;
            }
        };
    }

    private static String turnsLabel(int durationTurns) {
        return durationTurns == 1 ? "1 turn" : durationTurns + " turns";
    }

    private static String statChangeDescription(String stat, int power, int durationTurns) {
        return "+" + power + " " + stat + " for " + turnsLabel(durationTurns) + ".";
    }

    private static void requirePositive(int value, String field) {
        if (value <= 0) {
            throw new IllegalArgumentException(field + " must be positive");
        }
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
     * Passive percentage damage contribution while this effect is active
     * (e.g. 5 = +5%) — summed alongside equipment's own damage percent by
     * {@code EffectTarget.damagePercentBonus()}. 0 by default.
     */
    public int damagePercentBonus() {
        return 0;
    }

    /**
     * Whether this effect stops its holder from acting on their own turn at
     * all (e.g. Frozen) — checked by the combat turn engine, which skips
     * straight past a prevented holder for both a human player and the CPU
     * enemy. False by default.
     */
    public boolean preventsAction() {
        return false;
    }

    /**
     * Whether this effect counts as harmful to its holder — used by effects
     * like the Healer's ultimate that clear "all negative effects." False by
     * default (buffs); override true for debuffs (Frozen, Taunt, damage over
     * time, Golden Touch, ...).
     */
    public boolean isNegative() {
        return false;
    }

    /**
     * If non-null, the id of the only combatant this effect's holder is
     * allowed to choose as a SINGLE_ENEMY target — see Taunt. Null (no
     * restriction) by default.
     */
    public String forcedTargetId() {
        return null;
    }

    /**
     * Called on the holder right after they take {@code damage} from
     * attacker — mirrors {@code ItemPassive.onDamageTaken}, just for a
     * temporary effect instead of equipment (see Thorns, Golden Touch). No-op
     * by default.
     */
    public void onDamageTaken(EffectTarget holder, EffectTarget attacker, Damage damage) {
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
}
