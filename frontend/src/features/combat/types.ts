import type { CharacterClass } from '../party/types'
import type { EffectType } from '../classes/types'

export type CombatStatus = 'IN_PROGRESS' | 'PARTY_WON' | 'PARTY_LOST'

export interface ActiveEffect {
  type: EffectType
  power: number
  remainingTurns: number
}

export interface Combatant {
  id: string
  displayName: string
  enemy: boolean
  characterClass: CharacterClass | null
  maxHealth: number
  currentHealth: number
  maxStamina: number
  currentStamina: number
  ultimateCharge: number
  ultimateChargeThreshold: number
  alive: boolean
  activeEffects: ActiveEffect[]
}

export interface CombatState {
  code: string
  status: CombatStatus
  combatants: Combatant[]
  currentTurnCombatantId: string
}
