import { apiClient } from '../../shared/api/client'
import type { ClassDefinition } from './types'

export function getClassDefinitions(): Promise<ClassDefinition[]> {
  return apiClient.get<ClassDefinition[]>('/api/classes')
}
