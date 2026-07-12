import { Portal } from '../../../shared/ui/Portal'
import type { Combatant } from '../types'

interface Props {
  combatant: Combatant
  onClose: () => void
}

export function EnemyAttackInfo({ combatant, onClose }: Props) {
  if (!combatant.attackName || !combatant.attackEffectSummary) {
    return null
  }

  return (
    <Portal>
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
          <p className="ability-meta">{combatant.attackEffectSummary}</p>
        </div>
      </div>
    </Portal>
  )
}
