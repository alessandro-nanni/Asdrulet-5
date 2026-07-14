import { apiClient } from '../../shared/api/client'
import type { ItemDefinition } from './types'

export function getItemDefinitions(): Promise<ItemDefinition[]> {
  return apiClient.get<ItemDefinition[]>('/api/items')
}
