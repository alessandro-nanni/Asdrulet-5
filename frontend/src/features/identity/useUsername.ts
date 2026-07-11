import { useEffect, useState } from 'react'

const STORAGE_KEY = 'asdrulet5:username'

export function useUsername() {
  const [username, setUsername] = useState(() => localStorage.getItem(STORAGE_KEY) ?? '')

  useEffect(() => {
    if (username.trim()) {
      localStorage.setItem(STORAGE_KEY, username)
    }
  }, [username])

  return [username, setUsername] as const
}
