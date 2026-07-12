import type { Stats } from '../../classes/types'
import type { Combatant } from '../types'

interface Props {
  self: Combatant
  stats: Stats
}

export function SelfStatsPanel({ self, stats }: Props) {
  const healthPercent = Math.round((self.currentHealth / self.maxHealth) * 100)
  const staminaPercent = self.maxStamina > 0 ? Math.round((self.currentStamina / self.maxStamina) * 100) : 0
  const chargePercent =
    self.ultimateChargeThreshold > 0 ? Math.round((self.ultimateCharge / self.ultimateChargeThreshold) * 100) : 0
  const isUltimateReady = self.ultimateChargeThreshold > 0 && self.ultimateCharge >= self.ultimateChargeThreshold

  return (
    <div className="self-stats-panel">
      <div className="self-stat-row">
        <div className="self-stat self-stat-half">
          <div className="self-stat-label">
            <span>Health</span>
            <span>
              {self.currentHealth}/{self.maxHealth}
            </span>
          </div>
          <div className="combatant-bar combatant-bar-health">
            <div className="combatant-bar-fill" style={{ width: `${healthPercent}%` }} />
          </div>
        </div>
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
      </div>

      <div className="self-stat-grid">
        <div className="self-stat-tile" aria-label="Damage">
          <span aria-hidden="true">⚔️</span>
          <span className="self-stat-tile-value">{stats.damage}</span>
        </div>
        <div className="self-stat-tile" aria-label="Defense">
          <span aria-hidden="true">🛡️</span>
          <span className="self-stat-tile-value">{stats.defense}</span>
        </div>
      </div>

      <div className="self-stat">
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
  )
}
