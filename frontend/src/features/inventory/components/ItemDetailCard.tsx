import { ItemIcon } from './ItemIcon'
import { SLOT_LABELS } from '../types'
import type { ItemDefinition, PassiveEffect } from '../types'

const STAT_LABELS: Record<keyof PassiveEffect, string> = {
  bonusMaxHealth: 'Max Health',
  bonusMaxStamina: 'Max Stamina',
  bonusDefense: 'Defense',
  bonusDamage: 'Damage',
}

export function ItemDetailCard({ definition }: { definition: ItemDefinition }) {
  return (
    <>
      <div className="inventory-detail-header">
        <ItemIcon itemId={definition.id} className="inventory-detail-icon" />
        <div>
          <p className="ability-name">{definition.displayName}</p>
          <p className="eyebrow">{SLOT_LABELS[definition.slot]}</p>
        </div>
      </div>
      <p className="ability-description">{definition.description}</p>
      <ul className="inventory-stat-list">
        {(Object.keys(STAT_LABELS) as Array<keyof PassiveEffect>)
          .filter((stat) => definition.passiveEffect[stat] !== 0)
          .map((stat) => {
            const value = definition.passiveEffect[stat]
            return (
              <li key={stat} className={value > 0 ? 'is-positive' : 'is-negative'}>
                {STAT_LABELS[stat]} {value > 0 ? '+' : ''}
                {value}
              </li>
            )
          })}
      </ul>
    </>
  )
}
