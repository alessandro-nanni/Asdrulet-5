import goblinMarauder from '../../assets/enemies/goblin.png'
import caveRat from '../../assets/enemies/cave-rat.png'
import goblinSkirmisher from '../../assets/enemies/goblin-skirmisher.png'
import banditThug from '../../assets/enemies/bandit-thug.png'

// Keyed by EnemyDefinition.id (see backend EnemyDefinitionRegistry) rather
// than combatant id or displayName, since a fight's own "enemy-1"/"enemy-2"/...
// ids are per-fight and displayName may carry a disambiguating " 1"/" 2"/...
// suffix (see CombatService.toEnemyCombatants) — enemyDefinitionId is the
// stable per-species key, mirroring how items are keyed by itemId.
export const ENEMY_PORTRAITS: Record<string, string> = {
    'goblin-marauder': goblinMarauder,
    'cave-rat': caveRat,
    'goblin-skirmisher': goblinSkirmisher,
    'bandit-thug': banditThug,
}

// Unrecognized (or missing) enemyDefinitionId falls back to this rather
// than rendering nothing, mirroring how an unknown active-effect icon key
// falls back to a generic icon client-side (see CombatantCard's EFFECT_ICONS).
export const DEFAULT_ENEMY_PORTRAIT = goblinMarauder
