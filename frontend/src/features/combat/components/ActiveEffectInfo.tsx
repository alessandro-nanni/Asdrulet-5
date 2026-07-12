import type { ActiveEffect } from '../types'

const EFFECT_LABEL: Record<ActiveEffect['type'], string> = {
  BUFF_DEFENSE: 'Defense up',
  BUFF_DAMAGE: 'Damage up',
  DAMAGE: 'Damage',
  HEAL: 'Healing',
}

const EFFECT_VERB: Record<ActiveEffect['type'], string> = {
  BUFF_DEFENSE: 'defense',
  BUFF_DAMAGE: 'damage',
  DAMAGE: 'damage',
  HEAL: 'healing',
}

interface Props {
  effect: ActiveEffect
  onClose: () => void
}

export function ActiveEffectInfo({ effect, onClose }: Props) {
  const turnsLabel = effect.remainingTurns === 1 ? '1 more turn' : `${effect.remainingTurns} more turns`

  return (
    <div className="enemy-info-overlay" onClick={onClose}>
      <div className="enemy-info-card" onClick={(event) => event.stopPropagation()}>
        <div className="ability-card-header">
          <span className="ability-name">{EFFECT_LABEL[effect.type]}</span>
          <button type="button" className="icon-btn" onClick={onClose} aria-label="Close">
            ✕
          </button>
        </div>
        <p className="ability-description">
          +{effect.power} {EFFECT_VERB[effect.type]} for {turnsLabel}.
        </p>
      </div>
    </div>
  )
}
