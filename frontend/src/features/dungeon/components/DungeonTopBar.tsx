import type {ReactNode} from 'react'
import {HeartIcon} from '../../../shared/ui/HeartIcon'
import {MemberAvatar} from '../../party/components/MemberAvatar'
import type {PartyMember} from '../../party/types'

interface Props {
    member: PartyMember
    maxHealth: number
    // Currency pills (coins+inventory, mana+skill-tree) — rendered here,
    // top-right of the name row, rather than floating over the map, so both
    // sit next to each other in one place.
    actions?: ReactNode
}

export function DungeonTopBar({member, maxHealth, actions}: Props) {
    const currentHealth = member.currentHealth ?? maxHealth
    const healthPercent = maxHealth > 0 ? Math.round((currentHealth / maxHealth) * 100) : 0

    return (
        <div className="dungeon-topbar">
            <MemberAvatar member={member}/>
            <div className="dungeon-topbar-info">
                <div className="dungeon-topbar-top-row">
                    <span className="dungeon-topbar-name">{member.displayName}</span>
                    {actions && <div className="dungeon-topbar-actions">{actions}</div>}
                </div>
                <div className="dungeon-topbar-health">
                    <HeartIcon className="dungeon-topbar-health-icon"/>
                    <div className="combatant-bar combatant-bar-health dungeon-topbar-health-bar">
                        <div className="combatant-bar-fill" style={{width: `${healthPercent}%`}}/>
                    </div>
                    <span className="dungeon-topbar-health-value">
            {currentHealth}/{maxHealth}
          </span>
                </div>
            </div>
        </div>
    )
}
