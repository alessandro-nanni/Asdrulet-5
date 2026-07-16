import {apiClient} from '../../shared/api/client'
import type {PartyState} from '../party/types'
import type {SkillTree} from './types'

export function getSkillTrees(): Promise<SkillTree[]> {
    return apiClient.get<SkillTree[]>('/api/skill-trees')
}

export function unlockSkill(code: string, memberId: string, nodeId: string): Promise<PartyState> {
    return apiClient.post<PartyState>(`/api/parties/${code}/${memberId}/skills/unlock`, {nodeId})
}
