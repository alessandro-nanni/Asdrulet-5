import { apiClient, ApiError } from '../../shared/api/client'
import type { User } from './types'

export async function fetchCurrentUser(): Promise<User | null> {
  try {
    return await apiClient.get<User>('/api/me')
  } catch (error) {
    if (error instanceof ApiError && error.status === 401) {
      return null
    }
    throw error
  }
}
