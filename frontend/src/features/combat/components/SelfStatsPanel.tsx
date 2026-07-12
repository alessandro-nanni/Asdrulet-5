import type { Combatant } from '../types'

export function SelfStatsPanel({ self }: { self: Combatant }) {
  const healthPercent = Math.round((self.currentHealth / self.maxHealth) * 100)
  const staminaPercent = self.maxStamina > 0 ? Math.round((self.currentStamina / self.maxStamina) * 100) : 0
  const chargePercent =
    self.ultimateChargeThreshold > 0 ? Math.round((self.ultimateCharge / self.ultimateChargeThreshold) * 100) : 0

  return (
    <div className="self-stats-panel">
      <div className="self-stat">
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
      <div className="self-stat">
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
      <div className="self-stat">
        <div className="self-stat-label">
          <span>Ultimate</span>
          <span>
            {self.ultimateCharge}/{self.ultimateChargeThreshold}
          </span>
        </div>
        <div className="combatant-bar combatant-bar-charge">
          <div className="combatant-bar-fill" style={{ width: `${chargePercent}%` }} />
        </div>
      </div>
    </div>
  )
}
