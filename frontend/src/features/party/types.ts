export type CharacterClass = 'HEALER' | 'TANK' | 'WARRIOR' | 'MAGE'

export interface PartyMember {
  userId: string
  displayName: string
  avatarUrl: string
  characterClass: CharacterClass | null
  leader: boolean
}

export interface PartyState {
  code: string
  leaderId: string
  members: PartyMember[]
  turnOrder: string[]
}
