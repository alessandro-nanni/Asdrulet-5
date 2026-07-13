import { apiClient } from '../../shared/api/client'
import type { CharacterClass, PartyState } from './types'

export function createParty(displayName: string): Promise<PartyState> {
  return apiClient.post<PartyState>('/api/parties', { displayName })
}

export function joinParty(code: string, displayName: string): Promise<PartyState> {
  return apiClient.post<PartyState>(`/api/parties/${code}/join`, { displayName })
}

export function getParty(code: string): Promise<PartyState> {
  return apiClient.get<PartyState>(`/api/parties/${code}`)
}

export function selectClass(code: string, characterClass: CharacterClass): Promise<PartyState> {
  return apiClient.post<PartyState>(`/api/parties/${code}/class`, { characterClass })
}

export function startGame(code: string, memberIds: string[]): Promise<PartyState> {
  return apiClient.post<PartyState>(`/api/parties/${code}/start`, { memberIds })
}

export function enterCombat(code: string): Promise<PartyState> {
  return apiClient.post<PartyState>(`/api/parties/${code}/enter-combat`)
}
