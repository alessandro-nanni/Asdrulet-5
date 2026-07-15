import type { ComponentType, CSSProperties } from 'react'
import { useState } from 'react'
import { DEFAULT_ENEMY_PORTRAIT, ENEMY_PORTRAITS } from '../enemyPortraits'
import { FrozenIcon } from '../../../shared/ui/FrozenIcon'
import { GoldenTouchIcon } from '../../../shared/ui/GoldenTouchIcon'
import { HeartIcon } from '../../../shared/ui/HeartIcon'
import { PoisonIcon } from '../../../shared/ui/PoisonIcon'
import { ShieldIcon } from '../../../shared/ui/ShieldIcon'
import { SparkleIcon } from '../../../shared/ui/SparkleIcon'
import { StrengthIcon } from '../../../shared/ui/StrengthIcon'
import { SwordIcon } from '../../../shared/ui/SwordIcon'
import { TauntIcon } from '../../../shared/ui/TauntIcon'
import { ThornsIcon } from '../../../shared/ui/ThornsIcon'
import { MemberAvatar } from '../../party/components/MemberAvatar'
import type { PartyMember } from '../../party/types'
import type { ActiveEffect, Combatant } from '../types'
import { ActiveEffectInfo } from './ActiveEffectInfo'
import { EnemyInfoOverlay } from './EnemyInfoOverlay'

export interface FloatingText {
  key: string
  text: string
  kind: 'damage' | 'heal'
  offsetX: number
  delayMs: number
}

// Backend sends a stable icon key per effect (see ActiveEffect.java); unrecognized
// keys fall back to a generic icon rather than breaking, so new effects never
// require this map to be updated in lockstep with the backend.
const EFFECT_ICONS: Record<string, ComponentType<{ className?: string }>> = {
  shield: ShieldIcon,
  sword: SwordIcon,
  heal: HeartIcon,
  poison: PoisonIcon,
  frozen: FrozenIcon,
  strength: StrengthIcon,
  taunt: TauntIcon,
  thorns: ThornsIcon,
  goldenTouch: GoldenTouchIcon,
}

interface Props {
  combatant: Combatant
  member?: PartyMember
  isCurrentTurn: boolean
  isEngaged?: boolean
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
  isEngaged = false,
  selectable = false,
  isInvalidTarget = false,
  onSelect,
  reaction = null,
  isAttacking = false,
  floatingTexts = [],
}: Props) {
  const [showEnemyInfo, setShowEnemyInfo] = useState(false)
  const [activeEffectInfo, setActiveEffectInfo] = useState<ActiveEffect | null>(null)
  const healthPercent = Math.round((combatant.currentHealth / combatant.maxHealth) * 100)

  const classNames = [
    'combatant-card',
    isCurrentTurn ? 'is-current-turn' : '',
    isEngaged ? 'is-engaged' : '',
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
          <span
            key={floating.key}
            className={`floating-text floating-text-${floating.kind}`}
            style={
              {
                '--float-offset-x': `${floating.offsetX}px`,
                animationDelay: `${floating.delayMs}ms`,
              } as CSSProperties
            }
          >
            {floating.text}
          </span>
        ))}
      </div>
      {combatant.enemy && combatant.attackName && (
        <button
          type="button"
          className="combatant-info-btn"
          aria-label={`View ${combatant.displayName}'s info`}
          onClick={(event) => {
            event.stopPropagation()
            setShowEnemyInfo(true)
          }}
        >
          ?
        </button>
      )}
      {member ? (
        <MemberAvatar member={member} />
      ) : (
        <div className="enemy-portrait" aria-hidden="true">
          <img
            src={(combatant.enemyDefinitionId && ENEMY_PORTRAITS[combatant.enemyDefinitionId]) || DEFAULT_ENEMY_PORTRAIT}
            alt=""
            className="enemy-portrait-img"
          />
        </div>
      )}
      <div className="combatant-bar combatant-bar-health">
        <div className="combatant-bar-fill" style={{ width: `${healthPercent}%` }} />
        <span className="combatant-bar-overlay-label">{combatant.currentHealth}</span>
      </div>
      {combatant.activeEffects.length > 0 && (
        <div className="active-effects-row">
          {combatant.activeEffects.map((effect, index) => {
            const Icon = EFFECT_ICONS[effect.icon] ?? SparkleIcon
            return (
              <button
                key={`${effect.name}-${index}`}
                type="button"
                className="active-effect-badge"
                aria-label={`View active effect: ${effect.name}, ${effect.remainingTurns} turns left`}
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
      {showEnemyInfo && <EnemyInfoOverlay combatant={combatant} onClose={() => setShowEnemyInfo(false)} />}
      {activeEffectInfo && <ActiveEffectInfo effect={activeEffectInfo} onClose={() => setActiveEffectInfo(null)} />}
    </div>
  )
}
