import { HeartIcon } from '../../../shared/ui/HeartIcon'
import { ShieldIcon } from '../../../shared/ui/ShieldIcon'
import type { Stats } from '../../classes/types'
import type { Combatant } from '../types'

interface Props {
  self: Combatant
  stats: Stats
  isMyTurn: boolean
  hasSelectedAbility: boolean
  hasActedThisTurn: boolean
  isSubmitting: boolean
  onEndTurn: () => void
  onCancel: () => void
}

export function SelfStatsPanel({
  self,
  stats,
  isMyTurn,
  hasSelectedAbility,
  hasActedThisTurn,
  isSubmitting,
  onEndTurn,
  onCancel,
}: Props) {
  const staminaPercent = self.maxStamina > 0 ? Math.round((self.currentStamina / self.maxStamina) * 100) : 0
  const chargePercent =
    self.ultimateChargeThreshold > 0 ? Math.round((self.ultimateCharge / self.ultimateChargeThreshold) * 100) : 0
  const isUltimateReady = self.ultimateChargeThreshold > 0 && self.ultimateCharge >= self.ultimateChargeThreshold

  return (
    <div className="self-stats-panel">
      <div className="self-stat-top-row">
        <div className="self-stat-pill">
          <span className="self-stat-pill-item" aria-label={`Health ${self.currentHealth} of ${self.maxHealth}`}>
            <HeartIcon className="self-stat-tile-icon self-stat-tile-icon-health" />
            <span className="self-stat-tile-value">
              {self.currentHealth}/{self.maxHealth}
            </span>
          </span>
          <span className="self-stat-pill-item" aria-label={`Defense ${stats.defense}`}>
            <ShieldIcon className="self-stat-tile-icon" />
            <span className="self-stat-tile-value">{stats.defense}</span>
          </span>
        </div>

        {isMyTurn && (
          <button
            type="button"
            className="self-stat-end-turn-btn"
            disabled={isSubmitting}
            onClick={hasSelectedAbility ? onCancel : onEndTurn}
          >
            {hasSelectedAbility ? 'Cancel' : hasActedThisTurn ? 'End' : 'Skip'}
          </button>
        )}
      </div>

      <div className="self-stat-row">
        <div className="self-stat self-stat-half">
          <div className="self-stat-label">
            <span>Stamina</span>
            <span>
              {self.currentStamina}/{self.maxStamina}
            </span>
          </div>
          <div className="combatant-bar combatant-bar-stamina">
            <div className="combatant-bar-fill" style={{ width: `${staminaPercent}%` }} />
          </div>
        </div>
        <div className="self-stat self-stat-half">
          <div className="self-stat-label">
            <span>Ultimate</span>
            <span>
              {self.ultimateCharge}/{self.ultimateChargeThreshold}
            </span>
          </div>
          <div className={`ultimate-bar-track ${isUltimateReady ? 'is-charged' : ''}`}>
            <div className="ultimate-bar-fill" style={{ width: `${chargePercent}%` }} />
          </div>
        </div>
      </div>
    </div>
  )
}
