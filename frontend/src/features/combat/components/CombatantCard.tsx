import { MemberAvatar } from '../../party/components/MemberAvatar'
import type { PartyMember } from '../../party/types'
import type { Combatant } from '../types'

export interface FloatingText {
  key: string
  text: string
  kind: 'damage' | 'heal'
}

interface Props {
  combatant: Combatant
  member?: PartyMember
  isCurrentTurn: boolean
  selectable?: boolean
  onSelect?: () => void
  reaction?: 'hit' | 'healed' | null
  isAttacking?: boolean
  floatingTexts?: FloatingText[]
}

export function CombatantCard({
  combatant,
  member,
  isCurrentTurn,
  selectable = false,
  onSelect,
  reaction = null,
  isAttacking = false,
  floatingTexts = [],
}: Props) {
  const healthPercent = Math.round((combatant.currentHealth / combatant.maxHealth) * 100)
  const staminaPercent =
    combatant.maxStamina > 0 ? Math.round((combatant.currentStamina / combatant.maxStamina) * 100) : 0
  const chargePercent =
    combatant.ultimateChargeThreshold > 0
      ? Math.round((combatant.ultimateCharge / combatant.ultimateChargeThreshold) * 100)
      : 0

  const classNames = [
    'combatant-card',
    isCurrentTurn ? 'is-current-turn' : '',
    combatant.alive ? '' : 'is-defeated',
    selectable ? 'is-selectable' : '',
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
          <span key={floating.key} className={`floating-text floating-text-${floating.kind}`}>
            {floating.text}
          </span>
        ))}
      </div>
      {member ? (
        <MemberAvatar member={member} />
      ) : (
        <div className="enemy-portrait" aria-hidden="true">
          👹
        </div>
      )}
      <span className="combatant-name">{combatant.displayName}</span>
      <div className="combatant-bar combatant-bar-health">
        <div className="combatant-bar-fill" style={{ width: `${healthPercent}%` }} />
      </div>
      <span className="combatant-bar-label">
        {combatant.currentHealth}/{combatant.maxHealth}
      </span>
      {!combatant.enemy && (
        <>
          <div className="combatant-bar combatant-bar-stamina">
            <div className="combatant-bar-fill" style={{ width: `${staminaPercent}%` }} />
          </div>
          <div className="combatant-bar combatant-bar-charge">
            <div className="combatant-bar-fill" style={{ width: `${chargePercent}%` }} />
          </div>
        </>
      )}
    </div>
  )
}
