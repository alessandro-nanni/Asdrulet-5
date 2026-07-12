import type { Ability } from '../../classes/types'
import type { Combatant } from '../types'

interface Props {
  self: Combatant
  abilities: Ability[]
  selectedAbility: Ability | null
  hasActedThisTurn: boolean
  isSubmitting: boolean
  onSelectAbility: (abilityId: string) => void
  onCancel: () => void
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

function needsConfirmOnly(ability: Ability): boolean {
  return ability.targetType === 'ALL_ALLIES' || ability.targetType === 'ALL_ENEMIES'
}

export function AbilityActionPanel({
  self,
  abilities,
  selectedAbility,
  hasActedThisTurn,
  isSubmitting,
  onSelectAbility,
  onCancel,
  onConfirm,
  onEndTurn,
}: Props) {
  return (
    <div className="action-panel">
      <div className="ability-choice-list">
        {abilities.map((ability) => (
          <button
            key={ability.id}
            type="button"
            className={`ability-choice ${ability.type === 'ULTIMATE' ? 'is-ultimate' : ''} ${
              selectedAbility?.id === ability.id ? 'is-chosen' : ''
            }`}
            disabled={
              isSubmitting ||
              !canAfford(self, ability) ||
              (selectedAbility != null && selectedAbility.id !== ability.id)
            }
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

      {!selectedAbility && (
        <button type="button" className="btn btn-secondary btn-block" disabled={isSubmitting} onClick={onEndTurn}>
          {hasActedThisTurn ? 'End turn' : 'Skip turn'}
        </button>
      )}

      {selectedAbility && needsConfirmOnly(selectedAbility) && (
        <div className="action-confirm-row">
          <button type="button" className="btn btn-secondary" disabled={isSubmitting} onClick={onCancel}>
            Cancel
          </button>
          <button
            type="button"
            className="btn btn-primary"
            disabled={isSubmitting}
            onClick={() => onConfirm(selectedAbility.id)}
          >
            Use {selectedAbility.name}
          </button>
        </div>
      )}

      {selectedAbility && !needsConfirmOnly(selectedAbility) && (
        <div className="action-target-row">
          <p className="action-prompt">Tap a target for {selectedAbility.name}</p>
          <button type="button" className="btn btn-secondary btn-block" disabled={isSubmitting} onClick={onCancel}>
            Cancel
          </button>
        </div>
      )}
    </div>
  )
}
