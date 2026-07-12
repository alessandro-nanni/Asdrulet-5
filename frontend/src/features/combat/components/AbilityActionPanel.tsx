import { useState } from 'react'
import type { Ability } from '../../classes/types'
import type { Combatant } from '../types'
import { AbilityInfoOverlay } from './AbilityInfoOverlay'

interface Props {
  self: Combatant
  abilities: Ability[]
  selectedAbility: Ability | null
  isSubmitting: boolean
  onSelectAbility: (abilityId: string) => void
  onCancel: () => void
  onConfirm: (abilityId: string) => void
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
  isSubmitting,
  onSelectAbility,
  onCancel,
  onConfirm,
}: Props) {
  const [infoAbility, setInfoAbility] = useState<Ability | null>(null)

  return (
    <div className="action-panel">
      <div className="ability-choice-list">
        {abilities.map((ability) => (
          <div key={ability.id} className="ability-choice-wrap">
            <button
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
              <span className="ability-choice-name">{ability.name}</span>
              <span className="ability-choice-cost">
                {ability.type === 'ULTIMATE' ? `${self.ultimateCharge}/${self.ultimateChargeThreshold}` : ability.staminaCost}
              </span>
            </button>
            <button
              type="button"
              className="ability-info-btn"
              aria-label={`View ${ability.name} details`}
              onClick={(event) => {
                event.stopPropagation()
                setInfoAbility(ability)
              }}
            >
              ?
            </button>
          </div>
        ))}
      </div>

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

      {infoAbility && <AbilityInfoOverlay ability={infoAbility} onClose={() => setInfoAbility(null)} />}
    </div>
  )
}
