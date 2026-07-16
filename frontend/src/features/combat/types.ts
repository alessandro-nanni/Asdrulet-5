import type {CharacterClass} from '../party/types'
import type {Ability} from '../classes/types'

export type CombatStatus = 'IN_PROGRESS' | 'PARTY_WON' | 'PARTY_LOST'

export interface ActiveEffect {
    name: string
    description: string
    icon: string
    remainingTurns: number
}

export interface Combatant {
    id: string
    displayName: string
    enemy: boolean
    characterClass: CharacterClass | null
    // Which EnemyDefinition this enemy combatant was built from (e.g.
    // "cave-rat") — null for party members. Unlike id (this fight's own
    // "enemy-1"/"enemy-2"/...) or displayName (may carry a disambiguating
    // suffix), this is the stable per-species key to look up a portrait by —
    // see enemyPortraits.ts, mirroring how items are keyed by itemId.
    enemyDefinitionId: string | null
    maxHealth: number
    currentHealth: number
    maxStamina: number
    currentStamina: number
    defense: number
    ultimateCharge: number
    ultimateChargeThreshold: number
    alive: boolean
    actedThisTurn: boolean
    activeEffects: ActiveEffect[]
    attackName: string | null
    attackDescription: string | null
    attackEffectSummary: string | null
    totalDamageDealt: number
    totalHealingDone: number
    totalEffectsApplied: number
    // This combatant's own actual ability list — for a player, reflects
    // whatever they've unlocked in their class's skill tree, which the
    // static /api/classes catalog alone can't express (see BattleScreen).
    abilities: Ability[]
}

export interface CombatEvent {
    targetId: string
    kind: 'DAMAGE' | 'HEAL'
    amount: number
    // Always false for HEAL events — only a DAMAGE event can come from a
    // critical hit.
    critical: boolean
}

export interface CombatState {
    code: string
    status: CombatStatus
    combatants: Combatant[]
    currentTurnCombatantId: string
    recentEvents: CombatEvent[]
}
