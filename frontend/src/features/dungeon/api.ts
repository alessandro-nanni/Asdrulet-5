import {apiClient} from '../../shared/api/client'
import type {DungeonState} from './types'

export function getDungeon(code: string): Promise<DungeonState> {
    return apiClient.get<DungeonState>(`/api/parties/${code}/dungeon`)
}

export function selectNode(code: string, memberId: string, nodeId: string): Promise<DungeonState> {
    return apiClient.post<DungeonState>(`/api/parties/${code}/dungeon/${memberId}/select`, {nodeId})
}
