package com.asdru.asdrulet5.enemydata.domain;

import com.asdru.asdrulet5.classdata.domain.CombatantPassive;

/**
 * An enemy's own innate passive trait — the enemy-side counterpart to
 * {@code com.asdru.asdrulet5.inventory.domain.ItemPassive}. No enemy
 * currently defines one (see {@code EnemyDefinitionRegistry}), but the
 * hooks are already there via {@link CombatantPassive} for whenever a boss
 * or elite enemy needs its own lifesteal, thorns, enrage-on-low-health, or
 * similar behavior — no gear-specific stat bonuses here, since enemies
 * don't wear equipment.
 */
public interface EnemyPassive extends CombatantPassive {
}
