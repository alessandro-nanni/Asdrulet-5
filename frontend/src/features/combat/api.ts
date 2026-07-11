import { apiClient } from '../../shared/api/client'
import type { CombatState } from './types'

export function getCombat(code: string): Promise<CombatState> {
  return apiClient.get<CombatState>(`/api/parties/${code}/combat`)
}

export function useAbility(code: string, abilityId: string, targetId: string | null): Promise<CombatState> {
  return apiClient.post<CombatState>(`/api/parties/${code}/combat/actions`, { abilityId, targetId })
}

export function endTurn(code: string): Promise<CombatState> {
  return apiClient.post<CombatState>(`/api/parties/${code}/combat/end-turn`)
}

export function useAbilityAsFakeMember(
  code: string,
  memberId: string,
  abilityId: string,
  targetId: string | null,
): Promise<CombatState> {
  return apiClient.post<CombatState>(`/api/parties/${code}/combat/dev/${memberId}/actions`, { abilityId, targetId })
}

export function endTurnAsFakeMember(code: string, memberId: string): Promise<CombatState> {
  return apiClient.post<CombatState>(`/api/parties/${code}/combat/dev/${memberId}/end-turn`)
}
