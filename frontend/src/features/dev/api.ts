import { apiClient } from '../../shared/api/client'
import type { CharacterClass, PartyState } from '../party/types'
import type { DungeonState } from '../dungeon/types'

export function addFakeMembers(code: string, count: number): Promise<PartyState> {
  return apiClient.post<PartyState>(`/api/parties/${code}/dev/fake-members`, { count })
}

export function selectClassAsFakeMember(
  code: string,
  memberId: string,
  characterClass: CharacterClass,
): Promise<PartyState> {
  return apiClient.post<PartyState>(`/api/parties/${code}/dev/${memberId}/class`, { characterClass })
}

// The following talk to PartyDevSessionController, which lets a session-less
// "guest" identity (see ../guestIdentity.ts) create and drive a party without
// ever going through Google OAuth — used by QuickGameButton.

export function createDevParty(displayName: string): Promise<PartyState> {
  return apiClient.post<PartyState>('/api/parties/dev', { displayName })
}

export function selectClassAsMember(
  code: string,
  memberId: string,
  characterClass: CharacterClass,
): Promise<PartyState> {
  return apiClient.post<PartyState>(`/api/parties/dev/${code}/${memberId}/class`, { characterClass })
}

export function startGameAsMember(code: string, memberId: string, memberIds: string[]): Promise<PartyState> {
  return apiClient.post<PartyState>(`/api/parties/dev/${code}/${memberId}/start`, { memberIds })
}

export function enterRoomAsMember(code: string, memberId: string): Promise<PartyState> {
  return apiClient.post<PartyState>(`/api/parties/dev/${code}/${memberId}/enter-room`)
}

export function selectNodeAsMember(code: string, memberId: string, nodeId: string): Promise<DungeonState> {
  return apiClient.post<DungeonState>(`/api/parties/${code}/dungeon/dev/${memberId}/select`, { nodeId })
}
