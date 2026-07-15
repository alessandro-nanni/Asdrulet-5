import type {CharacterClass} from '../party/types'

export type TargetType = 'SELF' | 'SINGLE_ALLY' | 'ALL_ALLIES' | 'SINGLE_ENEMY' | 'ALL_ENEMIES'

export type AbilityKind = 'BASIC' | 'ULTIMATE'

export interface Stats {
    maxHealth: number
    defense: number
    speed: number
    maxStamina: number
}

export interface Ability {
    id: string
    name: string
    description: string
    effectSummary: string
    targetType: TargetType
    type: AbilityKind
    staminaCost: number | null
    chargeThreshold: number | null
}

export interface ClassDefinition {
    characterClass: CharacterClass
    displayName: string
    flavorText: string
    stats: Stats
    abilities: Ability[]
}
