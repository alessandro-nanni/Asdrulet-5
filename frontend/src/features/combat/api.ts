import { apiClient } from '../../shared/api/client'
import type { CombatState } from './types'

export function getCombat(code: string): Promise<CombatState> {
  return apiClient.get<CombatState>(`/api/parties/${code}/combat`)
}

export function useAbility(
  code: string,
  memberId: string,
  abilityId: string,
  targetId: string | null,
): Promise<CombatState> {
  return apiClient.post<CombatState>(`/api/parties/${code}/combat/${memberId}/actions`, { abilityId, targetId })
}

export function endTurn(code: string, memberId: string): Promise<CombatState> {
  return apiClient.post<CombatState>(`/api/parties/${code}/combat/${memberId}/end-turn`)
}
