export type ItemSlot = 'WEAPON' | 'CHESTPLATE' | 'TRINKET' | 'CONSUMABLE'

export const SLOT_LABELS: Record<ItemSlot, string> = {
    WEAPON: 'Weapon',
    CHESTPLATE: 'Chestplate',
    TRINKET: 'Trinket',
    CONSUMABLE: 'Consumable',
}

export interface PassiveEffect {
    bonusMaxHealth: number
    bonusMaxStamina: number
    bonusDefense: number
    damagePercent: number
}

export interface ItemDefinition {
    id: string
    displayName: string
    slot: ItemSlot
    description: string
    passiveEffect: PassiveEffect
    price: number
    // Health restored on use — 0 for every non-CONSUMABLE item.
    healAmount: number
}
