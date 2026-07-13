export type CharacterClass = 'HEALER' | 'TANK' | 'WARRIOR' | 'MAGE'

export type PartyStatus = 'LOBBY' | 'DUNGEON' | 'IN_PROGRESS'

export interface PartyMember {
  userId: string
  displayName: string
  avatarUrl: string
  characterClass: CharacterClass | null
  leader: boolean
  bot: boolean
}

export interface PartyState {
  code: string
  leaderId: string
  members: PartyMember[]
  turnOrder: string[]
  status: PartyStatus
}
