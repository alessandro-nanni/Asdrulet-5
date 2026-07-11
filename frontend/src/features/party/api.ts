import { apiClient } from '../../shared/api/client'
import type { CharacterClass, PartyState } from './types'

export function createParty(): Promise<PartyState> {
  return apiClient.post<PartyState>('/api/parties')
}

export function joinParty(code: string): Promise<PartyState> {
  return apiClient.post<PartyState>(`/api/parties/${code}/join`)
}

export function getParty(code: string): Promise<PartyState> {
  return apiClient.get<PartyState>(`/api/parties/${code}`)
}

export function selectClass(code: string, characterClass: CharacterClass): Promise<PartyState> {
  return apiClient.post<PartyState>(`/api/parties/${code}/class`, { characterClass })
}

export function setTurnOrder(code: string, memberIds: string[]): Promise<PartyState> {
  return apiClient.post<PartyState>(`/api/parties/${code}/turn-order`, { memberIds })
}
