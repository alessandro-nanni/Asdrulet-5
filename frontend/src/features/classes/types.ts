import type { CharacterClass } from '../party/types'

export type TargetType = 'SELF' | 'SINGLE_ALLY' | 'ALL_ALLIES' | 'SINGLE_ENEMY' | 'ALL_ENEMIES'

export type AbilityKind = 'BASIC' | 'ULTIMATE'

export type EffectType = 'DAMAGE' | 'HEAL' | 'BUFF_DEFENSE' | 'BUFF_DAMAGE'

export interface Stats {
  maxHealth: number
  damage: number
  defense: number
  speed: number
  maxStamina: number
}

export interface Effect {
  type: EffectType
  power: number
  durationTurns: number
}

export interface Ability {
  id: string
  name: string
  description: string
  targetType: TargetType
  type: AbilityKind
  staminaCost: number | null
  chargeThreshold: number | null
  effect: Effect
}

export interface ClassDefinition {
  characterClass: CharacterClass
  displayName: string
  flavorText: string
  stats: Stats
  abilities: Ability[]
}
