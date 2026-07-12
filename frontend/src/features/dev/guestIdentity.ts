const STORAGE_PREFIX = 'asdrulet5:dev-self:'

export function getGuestSelfId(code: string): string | null {
  return localStorage.getItem(STORAGE_PREFIX + code)
}

export function setGuestSelfId(code: string, memberId: string): void {
  localStorage.setItem(STORAGE_PREFIX + code, memberId)
}
