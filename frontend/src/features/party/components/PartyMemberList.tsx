import type { PartyMember } from '../types'

export function PartyMemberList({ members }: { members: PartyMember[] }) {
  return (
    <ul className="member-list">
      {members.map((member) => (
        <li key={member.userId} className="member-item">
          <span className="member-name">
            {member.displayName}
            {member.leader && <span className="badge badge-leader">Leader</span>}
          </span>
          {member.characterClass ? (
            <span className={`badge badge-class class-${member.characterClass.toLowerCase()}`}>
              {member.characterClass}
            </span>
          ) : (
            <span className="badge badge-pending">Choosing...</span>
          )}
        </li>
      ))}
    </ul>
  )
}
