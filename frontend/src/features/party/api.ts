import { apiClient } from '../../shared/api/client'
import type { CharacterClass, PartyState } from './types'

export function createParty(id: string, displayName: string, avatarUrl: string | null): Promise<PartyState> {
  return apiClient.post<PartyState>('/api/parties', { id, displayName, avatarUrl })
}

export function joinParty(
  code: string,
  memberId: string,
  displayName: string,
  avatarUrl: string | null,
): Promise<PartyState> {
  return apiClient.post<PartyState>(`/api/parties/${code}/${memberId}/join`, { displayName, avatarUrl })
}

export function getParty(code: string): Promise<PartyState> {
  return apiClient.get<PartyState>(`/api/parties/${code}`)
}

export function selectClass(code: string, memberId: string, characterClass: CharacterClass): Promise<PartyState> {
  return apiClient.post<PartyState>(`/api/parties/${code}/${memberId}/class`, { characterClass })
}

export function startGame(code: string, memberId: string, memberIds: string[]): Promise<PartyState> {
  return apiClient.post<PartyState>(`/api/parties/${code}/${memberId}/start`, { memberIds })
}

export function enterRoom(code: string, memberId: string): Promise<PartyState> {
  return apiClient.post<PartyState>(`/api/parties/${code}/${memberId}/enter-room`)
}

export function equipFromStorage(code: string, memberId: string, storageIndex: number): Promise<PartyState> {
  return apiClient.post<PartyState>(`/api/parties/${code}/${memberId}/inventory/equip-from-storage`, { storageIndex })
}

export function spinWheel(code: string, memberId: string): Promise<PartyState> {
  return apiClient.post<PartyState>(`/api/parties/${code}/${memberId}/wheel/spin`)
}

export function acknowledgeWheelResult(code: string, memberId: string): Promise<PartyState> {
  return apiClient.post<PartyState>(`/api/parties/${code}/${memberId}/wheel/acknowledge`)
}
