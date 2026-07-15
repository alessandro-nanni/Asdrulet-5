export type CharacterClass = 'HEALER' | 'PALADIN' | 'BERSERKER' | 'MAGE'

export type PartyStatus = 'LOBBY' | 'DUNGEON' | 'IN_PROGRESS'

export type WheelEffect = 'FULL_HEAL' | 'HALVE_HEALTH' | 'GIVE_ITEM' | 'GIVE_COINS' | 'CLEAR_EFFECTS' | 'POISON'

export interface Loadout {
    weaponItemId: string | null
    chestplateItemId: string | null
    trinketItemId: string | null
}

// Same shape as combat's ActiveEffect (features/combat/types.ts) — kept as a
// separate type rather than imported from there, mirroring how the backend
// maps its own party.web.dto.PendingEffectDto instead of reusing combat's.
export interface PendingEffect {
    name: string
    description: string
    icon: string
    remainingTurns: number
}

// What one member found opening a LOOT room's chest — coins, one or more
// items, or both (itemIds is empty when the roll didn't include one).
export interface LootResult {
    coins: number
    itemIds: string[]
}

export interface PartyMember {
    userId: string
    displayName: string
    avatarUrl: string
    characterClass: CharacterClass | null
    leader: boolean
    bot: boolean
    loadout: Loadout
    // null means "at full health" — see the backend PartyMember's own doc.
    currentHealth: number | null
    // Carried into the member's next fight as real ActiveEffects (e.g. a
    // MYSTERY wheel's poison) — see the backend PartyMember's own doc.
    pendingEffects: PendingEffect[]
}

export interface PartyState {
    code: string
    leaderId: string
    members: PartyMember[]
    turnOrder: string[]
    status: PartyStatus
    // 12 cells (3 columns x 4 rows), each either an item id or null.
    storage: (string | null)[]
    // Keyed by userId — only ever holds entries for the MYSTERY room currently
    // entered, reset the moment a new room is entered.
    wheelResults: Record<string, WheelEffect>
    // Shared party-wide pool, spent from the shop — not per-member.
    coins: number
    // Item ids currently for sale — only holds entries for the MERCHANT room
    // currently entered, reset the moment a new room is entered.
    shopStock: string[]
    // Keyed by userId — only ever holds entries for the LOOT room currently
    // entered, reset the moment a new room is entered.
    lootResults: Record<string, LootResult>
}
