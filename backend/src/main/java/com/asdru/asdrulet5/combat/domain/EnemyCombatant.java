package com.asdru.asdrulet5.combat.domain;

import com.asdru.asdrulet5.classdata.domain.Ability;
import com.asdru.asdrulet5.classdata.domain.Stats;
import com.asdru.asdrulet5.enemydata.domain.EnemyPassive;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * An enemy's side of a fight. {@link #id()} is which {@code EnemyDefinition}
 * this came from (e.g. "cave-rat") — distinct from the inherited
 * {@link #combatantId()} (this fight's own "enemy-1"/"enemy-2"/...), since
 * an encounter can field more than one of the same species (see
 * CombatService.toEnemyCombatants) and only combatantId needs to stay
 * unique for targeting.
 */
@Getter
@Accessors(fluent = true)
public final class EnemyCombatant extends Combatant {

    private final String id;

    public EnemyCombatant(String combatantId, String displayName, String id, Stats stats,
                          int ultimateChargeThreshold, List<Ability> abilities, List<EnemyPassive> passives) {
        super(combatantId, displayName, stats, ultimateChargeThreshold, abilities, passives);
        this.id = id;
    }

    @Override
    public boolean enemy() {
        return true;
    }
}
