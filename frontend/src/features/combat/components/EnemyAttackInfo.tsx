import type { Effect } from '../../classes/types'
import type { Combatant } from '../types'

const EFFECT_LABEL: Record<Effect['type'], (effect: Effect) => string> = {
  DAMAGE: (effect) => `${effect.power} damage`,
  HEAL: (effect) => `${effect.power} healing`,
  BUFF_DEFENSE: (effect) => `+${effect.power} defense for ${effect.durationTurns} turns`,
  BUFF_DAMAGE: (effect) => `+${effect.power} damage for ${effect.durationTurns} turns`,
}

interface Props {
  combatant: Combatant
  onClose: () => void
}

export function EnemyAttackInfo({ combatant, onClose }: Props) {
  if (!combatant.attackName || !combatant.attackEffect) {
    return null
  }

  return (
    <div className="enemy-info-overlay" onClick={onClose}>
      <div className="enemy-info-card" onClick={(event) => event.stopPropagation()}>
        <div className="ability-card-header">
          <span className="ability-name">{combatant.displayName}</span>
          <button type="button" className="icon-btn" onClick={onClose} aria-label="Close">
            ✕
          </button>
        </div>
        <p className="enemy-attack-name">{combatant.attackName}</p>
        <p className="ability-description">{combatant.attackDescription}</p>
        <p className="ability-meta">{EFFECT_LABEL[combatant.attackEffect.type](combatant.attackEffect)}</p>
      </div>
    </div>
  )
}
