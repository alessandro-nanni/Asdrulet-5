import type { PartyMember } from '../types'

export function PartyMemberList({ members }: { members: PartyMember[] }) {
  return (
    <ul>
      {members.map((member) => (
        <li key={member.userId}>
          {member.displayName}
          {member.leader ? ' (Leader)' : ''} &mdash; {member.characterClass ?? 'No class selected'}
        </li>
      ))}
    </ul>
  )
}
