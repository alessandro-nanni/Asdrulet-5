import {apiClient} from '../../shared/api/client'
import type {CharacterClass, PartyState} from '../party/types'

// Talks to PartyDevController: dev-only tooling (gated behind
// app.dev-tools.enabled on the server) for populating a party with
// locally-simulated bot members, so multi-member flows can be tested from a
// single browser. Unrelated to identity — every real member (bot or not)
// uses the same unified party/dungeon/combat APIs.

export function addFakeMembers(code: string, count: number): Promise<PartyState> {
    return apiClient.post<PartyState>(`/api/parties/${code}/dev/fake-members`, {count})
}

export function selectClassAsFakeMember(
    code: string,
    memberId: string,
    characterClass: CharacterClass,
): Promise<PartyState> {
    return apiClient.post<PartyState>(`/api/parties/${code}/dev/${memberId}/class`, {characterClass})
}
