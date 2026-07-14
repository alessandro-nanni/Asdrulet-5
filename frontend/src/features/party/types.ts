export type CharacterClass = 'HEALER' | 'TANK' | 'WARRIOR' | 'MAGE'

export type PartyStatus = 'LOBBY' | 'DUNGEON' | 'IN_PROGRESS'

export interface Loadout {
  weaponItemId: string | null
  chestplateItemId: string | null
  trinketItemId: string | null
}

export interface PartyMember {
  userId: string
  displayName: string
  avatarUrl: string
  characterClass: CharacterClass | null
  leader: boolean
  bot: boolean
  loadout: Loadout
}

export interface PartyState {
  code: string
  leaderId: string
  members: PartyMember[]
  turnOrder: string[]
  status: PartyStatus
  // 12 cells (3 columns x 4 rows), each either an item id or null.
  storage: (string | null)[]
}
