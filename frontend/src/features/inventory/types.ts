export type ItemSlot = 'WEAPON' | 'CHESTPLATE' | 'TRINKET'

export const SLOT_LABELS: Record<ItemSlot, string> = {
  WEAPON: 'Weapon',
  CHESTPLATE: 'Chestplate',
  TRINKET: 'Trinket',
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
}
