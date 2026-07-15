import type {PartyMember} from '../types'
import {ClassHat} from './ClassHat'

export function MemberAvatar({member}: { member: PartyMember }) {
    return (
        <div className="member-avatar-wrap">
            {member.avatarUrl ? (
                <img src={member.avatarUrl} alt="" className="member-avatar" referrerPolicy="no-referrer"/>
            ) : (
                <div className="member-avatar member-avatar-fallback" aria-hidden="true">
                    {member.displayName.charAt(0).toUpperCase()}
                </div>
            )}
            {member.characterClass && <ClassHat characterClass={member.characterClass}/>}
        </div>
    )
}
