import type { CharacterClass } from '../party/types'

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
  maxHealth: number
  currentHealth: number
  maxStamina: number
  currentStamina: number
  defense: number
  ultimateCharge: number
  ultimateChargeThreshold: number
  alive: boolean
  activeEffects: ActiveEffect[]
  attackName: string | null
  attackDescription: string | null
  attackEffectSummary: string | null
}

export interface CombatEvent {
  targetId: string
  kind: 'DAMAGE' | 'HEAL'
  amount: number
}

export interface CombatState {
  code: string
  status: CombatStatus
  combatants: Combatant[]
  currentTurnCombatantId: string
  recentEvents: CombatEvent[]
}
