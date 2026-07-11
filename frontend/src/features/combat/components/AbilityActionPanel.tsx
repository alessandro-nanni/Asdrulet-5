import type { Ability } from '../../classes/types'
import type { Combatant } from '../types'

interface Props {
  self: Combatant
  abilities: Ability[]
  selectedAbility: Ability | null
  hasActedThisTurn: boolean
  onSelectAbility: (abilityId: string) => void
  onUndo: () => void
  onConfirm: (abilityId: string) => void
  onEndTurn: () => void
}

const EFFECT_ICONS: Record<Ability['effect']['type'], string> = {
  DAMAGE: '⚔️',
  HEAL: '✨',
  BUFF_DEFENSE: '🛡️',
  BUFF_DAMAGE: '💪',
}

function canAfford(self: Combatant, ability: Ability): boolean {
  if (ability.type === 'ULTIMATE') {
    return self.ultimateCharge >= self.ultimateChargeThreshold
  }
  return self.currentStamina >= (ability.staminaCost ?? 0)
}

function needsTargetPicker(ability: Ability): boolean {
  return ability.targetType === 'SINGLE_ALLY' || ability.targetType === 'SINGLE_ENEMY'
}

export function AbilityActionPanel({
  self,
  abilities,
  selectedAbility,
  hasActedThisTurn,
  onSelectAbility,
  onUndo,
  onConfirm,
  onEndTurn,
}: Props) {
  if (!selectedAbility) {
    return (
      <div className="action-panel">
        <div className="ability-choice-list">
          {abilities.map((ability) => (
            <button
              key={ability.id}
              type="button"
              className={`ability-choice ${ability.type === 'ULTIMATE' ? 'is-ultimate' : ''}`}
              disabled={!canAfford(self, ability)}
              onClick={() => onSelectAbility(ability.id)}
            >
              <span className="ability-choice-icon" aria-hidden="true">
                {EFFECT_ICONS[ability.effect.type]}
              </span>
              <span className="ability-choice-name">{ability.name}</span>
              <span className="ability-choice-cost">
                {ability.type === 'ULTIMATE' ? `${self.ultimateCharge}/${self.ultimateChargeThreshold}` : ability.staminaCost}
              </span>
            </button>
          ))}
        </div>
        <button type="button" className="btn btn-secondary btn-block" onClick={onEndTurn}>
          {hasActedThisTurn ? 'End turn' : 'Skip turn'}
        </button>
      </div>
    )
  }

  if (needsTargetPicker(selectedAbility)) {
    return (
      <div className="action-panel">
        <p className="action-prompt">Tap a target on the field for {selectedAbility.name}</p>
        <button type="button" className="btn btn-secondary btn-block" onClick={onUndo}>
          Undo
        </button>
      </div>
    )
  }

  return (
    <div className="action-panel">
      <p className="action-prompt">Use {selectedAbility.name}?</p>
      <div className="action-confirm-row">
        <button type="button" className="btn btn-secondary" onClick={onUndo}>
          Undo
        </button>
        <button type="button" className="btn btn-primary" onClick={() => onConfirm(selectedAbility.id)}>
          Confirm
        </button>
      </div>
    </div>
  )
}
