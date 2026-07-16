import type {CharacterClass} from '../party/types'
import type {Ability} from '../classes/types'

export type SkillNodeEffectKind = 'UPGRADE' | 'ADD'

export interface SkillNode {
    id: string
    name: string
    description: string
    manaCost: number
    // null for a tree's root node.
    parentId: string | null
    effectKind: SkillNodeEffectKind
    // The ability as it looks once this node is unlocked — the base ability
    // being upgraded, or the new one being added.
    resultingAbility: Ability
}

export interface SkillTree {
    characterClass: CharacterClass
    nodes: SkillNode[]
}
