import { useState } from 'react'
import { useItemDefinitions } from '../../inventory/useItemDefinitions'
import { ItemIcon } from '../../inventory/components/ItemIcon'
import { ItemInfoOverlay } from '../../inventory/components/ItemInfoOverlay'
import type { Loadout } from '../../party/types'

interface Props {
  loadout: Loadout
}

const LOADOUT_FIELDS: Array<keyof Loadout> = ['weaponItemId', 'chestplateItemId', 'trinketItemId']

export function EquippedItemsPanel({ loadout }: Props) {
  const { definitions } = useItemDefinitions()
  const [selectedItemId, setSelectedItemId] = useState<string | null>(null)

  const equippedItemIds = LOADOUT_FIELDS.map((field) => loadout[field]).filter(
    (itemId): itemId is string => itemId != null,
  )

  if (equippedItemIds.length === 0) {
    return null
  }

  const selectedDefinition = definitions.find((definition) => definition.id === selectedItemId) ?? null

  return (
    <div className="equipped-items-panel">
      <p className="equipped-items-label">Equipped</p>
      <div className="equipped-items-list">
        {equippedItemIds.map((itemId) => {
          const definition = definitions.find((candidate) => candidate.id === itemId)
          return (
            <button
              key={itemId}
              type="button"
              className="equipped-item-btn"
              onClick={() => setSelectedItemId(itemId)}
              aria-label={definition?.displayName ?? itemId}
            >
              <ItemIcon itemId={itemId} className="equipped-item-icon" />
            </button>
          )
        })}
      </div>

      {selectedDefinition && <ItemInfoOverlay definition={selectedDefinition} onClose={() => setSelectedItemId(null)} />}
    </div>
  )
}
