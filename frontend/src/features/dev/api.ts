import { apiClient } from '../../shared/api/client'
import type { CharacterClass, PartyState } from '../party/types'

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
