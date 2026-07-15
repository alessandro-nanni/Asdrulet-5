import {MemberAvatar} from './MemberAvatar'
import {CrownIcon} from '../../../shared/ui/CrownIcon'
import type {PartyMember} from '../types'

export function PartyMemberList({members}: { members: PartyMember[] }) {
    return (
        <ul className="member-list">
            {members.map((member) => (
                <li key={member.userId} className="member-item">
                    <div className="member-identity">
                        <MemberAvatar member={member}/>
                        <span className="member-name">
              {member.displayName}
                            {member.leader && <CrownIcon className="crown-icon"/>}
            </span>
                    </div>
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
