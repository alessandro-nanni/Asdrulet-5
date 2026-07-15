// Cheap structural-equality check for small JSON-shaped state snapshots
// (party/combat state). Used to dedupe when the same state arrives twice
// through different channels — e.g. a mutation's own REST response applied
// immediately, followed moments later by the WebSocket broadcast of that
// same change — so the second arrival doesn't trigger a redundant re-render
// (and, in combat, redundant animations).
export function jsonEqual<T>(a: T, b: T): boolean {
    return JSON.stringify(a) === JSON.stringify(b)
}
