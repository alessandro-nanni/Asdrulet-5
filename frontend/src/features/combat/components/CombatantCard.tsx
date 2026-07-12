import type { ComponentType } from 'react'
import { useState } from 'react'
import { HeartIcon } from '../../../shared/ui/HeartIcon'
import { ShieldIcon } from '../../../shared/ui/ShieldIcon'
import { SwordIcon } from '../../../shared/ui/SwordIcon'
import { MemberAvatar } from '../../party/components/MemberAvatar'
import type { PartyMember } from '../../party/types'
import type { ActiveEffect, Combatant } from '../types'
import { ActiveEffectInfo } from './ActiveEffectInfo'
import { EnemyAttackInfo } from './EnemyAttackInfo'

const ACTIVE_EFFECT_ICON: Record<ActiveEffect['type'], ComponentType<{ className?: string }>> = {
  BUFF_DEFENSE: ShieldIcon,
  BUFF_DAMAGE: SwordIcon,
  DAMAGE: SwordIcon,
  HEAL: HeartIcon,
}

export interface FloatingText {
  key: string
  text: string
  kind: 'damage' | 'heal'
}

interface Props {
  combatant: Combatant
  member?: PartyMember
  isCurrentTurn: boolean
  selectable?: boolean
  isInvalidTarget?: boolean
  onSelect?: () => void
  reaction?: 'hit' | 'healed' | null
  isAttacking?: boolean
  floatingTexts?: FloatingText[]
}

export function CombatantCard({
  combatant,
  member,
  isCurrentTurn,
  selectable = false,
  isInvalidTarget = false,
  onSelect,
  reaction = null,
  isAttacking = false,
  floatingTexts = [],
}: Props) {
  const [showAttackInfo, setShowAttackInfo] = useState(false)
  const [activeEffectInfo, setActiveEffectInfo] = useState<ActiveEffect | null>(null)
  const healthPercent = Math.round((combatant.currentHealth / combatant.maxHealth) * 100)

  const classNames = [
    'combatant-card',
    isCurrentTurn ? 'is-current-turn' : '',
    combatant.alive ? '' : 'is-defeated',
    selectable ? 'is-selectable' : '',
    isInvalidTarget ? 'is-invalid-target' : '',
    isAttacking ? 'is-attacking' : '',
    reaction === 'hit' ? 'is-hit' : '',
    reaction === 'healed' ? 'is-healed' : '',
  ]
    .filter(Boolean)
    .join(' ')

  return (
    <div
      className={classNames}
      onClick={selectable ? onSelect : undefined}
      role={selectable ? 'button' : undefined}
      tabIndex={selectable ? 0 : undefined}
    >
      <div className="combatant-fx-layer" aria-hidden="true">
        {floatingTexts.map((floating) => (
          <span key={floating.key} className={`floating-text floating-text-${floating.kind}`}>
            {floating.text}
          </span>
        ))}
      </div>
      {combatant.enemy && combatant.attackName && (
        <button
          type="button"
          className="combatant-info-btn"
          aria-label={`View ${combatant.displayName}'s attack`}
          onClick={(event) => {
            event.stopPropagation()
            setShowAttackInfo(true)
          }}
        >
          ?
        </button>
      )}
      {member ? (
        <MemberAvatar member={member} />
      ) : (
        <div className="enemy-portrait" aria-hidden="true">
          👹
        </div>
      )}
      <div className="combatant-bar combatant-bar-health">
        <div className="combatant-bar-fill" style={{ width: `${healthPercent}%` }} />
        <span className="combatant-bar-overlay-label">{combatant.currentHealth}</span>
      </div>
      {combatant.activeEffects.length > 0 && (
        <div className="active-effects-row">
          {combatant.activeEffects.map((effect, index) => {
            const Icon = ACTIVE_EFFECT_ICON[effect.type]
            return (
              <button
                key={`${effect.type}-${index}`}
                type="button"
                className="active-effect-badge"
                aria-label={`View active effect: +${effect.power} ${effect.type.replace('_', ' ').toLowerCase()}, ${effect.remainingTurns} turns left`}
                onClick={(event) => {
                  event.stopPropagation()
                  setActiveEffectInfo(effect)
                }}
              >
                <Icon className="active-effect-icon" />
                <span className="active-effect-turns">{effect.remainingTurns}</span>
              </button>
            )
          })}
        </div>
      )}
      {showAttackInfo && <EnemyAttackInfo combatant={combatant} onClose={() => setShowAttackInfo(false)} />}
      {activeEffectInfo && <ActiveEffectInfo effect={activeEffectInfo} onClose={() => setActiveEffectInfo(null)} />}
    </div>
  )
}
