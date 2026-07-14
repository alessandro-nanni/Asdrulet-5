import { useMemo, useState } from 'react'
import { Portal } from '../../../shared/ui/Portal'
import { equipFromStorage } from '../../party/api'
import { useItemDefinitions } from '../useItemDefinitions'
import { ItemIcon } from './ItemIcon'
import { ItemDetailCard } from './ItemDetailCard'
import { SLOT_LABELS } from '../types'
import type { Loadout, PartyMember, PartyState } from '../../party/types'
import type { ItemDefinition, ItemSlot } from '../types'

interface Props {
  code: string
  member: PartyMember
  storage: (string | null)[]
  onApplyUpdate: (state: PartyState) => void
  onClose: () => void
}

interface Selection {
  itemId: string
  // Index of the storage cell this item was tapped from, or null if it was
  // tapped from the equipped row below — only a storage-sourced selection
  // can be equipped from here.
  storageIndex: number | null
}

const SLOT_ORDER: ItemSlot[] = ['WEAPON', 'CHESTPLATE', 'TRINKET']

const LOADOUT_FIELD: Record<ItemSlot, keyof Loadout> = {
  WEAPON: 'weaponItemId',
  CHESTPLATE: 'chestplateItemId',
  TRINKET: 'trinketItemId',
}

export function InventoryScreen({ code, member, storage, onApplyUpdate, onClose }: Props) {
  const { definitions } = useItemDefinitions()
  const [selected, setSelected] = useState<Selection | null>(null)
  const [isEquipping, setIsEquipping] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const definitionsById = useMemo(
    () => new Map(definitions.map((definition) => [definition.id, definition])),
    [definitions],
  )
  const selectedDefinition = selected ? (definitionsById.get(selected.itemId) ?? null) : null
  const canEquipSelected = selected != null && selected.storageIndex !== null

  async function handleEquip() {
    if (!selected || selected.storageIndex === null || isEquipping) {
      return
    }
    setError(null)
    setIsEquipping(true)
    try {
      onApplyUpdate(await equipFromStorage(code, member.userId, selected.storageIndex))
      // The item just moved into the equipped slot, so it's no longer
      // equippable from a storage index — reflects that immediately rather
      // than waiting on the party state round-trip.
      setSelected({ itemId: selected.itemId, storageIndex: null })
    } catch {
      setError('Could not equip that item. Try again.')
    } finally {
      setIsEquipping(false)
    }
  }

  return (
    <Portal>
      <div className="inventory-screen">
        <div className="inventory-header">
          <h2 className="section-title">Inventory</h2>
          <button type="button" className="icon-btn" onClick={onClose} aria-label="Close inventory">
            ✕
          </button>
        </div>

        <ItemDetailPanel
          definition={selectedDefinition}
          canEquip={canEquipSelected}
          isEquipping={isEquipping}
          onEquip={handleEquip}
          error={error}
        />

        <div className="inventory-storage-scroll">
          <div className="inventory-storage-grid">
            {storage.map((itemId, index) => (
              <button
                key={index}
                type="button"
                className={[
                  'inventory-cell',
                  itemId ? '' : 'is-empty',
                  itemId && itemId === selected?.itemId ? 'is-selected' : '',
                ]
                  .filter(Boolean)
                  .join(' ')}
                disabled={!itemId}
                onClick={() => {
                  if (itemId) {
                    setSelected({ itemId, storageIndex: index })
                    setError(null)
                  }
                }}
                aria-label={itemId ? (definitionsById.get(itemId)?.displayName ?? itemId) : 'Empty storage cell'}
              >
                {itemId && <ItemIcon itemId={itemId} className="inventory-cell-icon" />}
              </button>
            ))}
          </div>
        </div>

        <div className="inventory-equipped-row">
          {SLOT_ORDER.map((slot) => {
            const itemId = member.loadout[LOADOUT_FIELD[slot]]
            return (
              <button
                key={slot}
                type="button"
                className={[
                  'inventory-cell',
                  'inventory-equipped-cell',
                  itemId ? '' : 'is-empty',
                  itemId && itemId === selected?.itemId ? 'is-selected' : '',
                ]
                  .filter(Boolean)
                  .join(' ')}
                disabled={!itemId}
                onClick={() => {
                  if (itemId) {
                    setSelected({ itemId, storageIndex: null })
                    setError(null)
                  }
                }}
                aria-label={itemId ? (definitionsById.get(itemId)?.displayName ?? itemId) : `No ${SLOT_LABELS[slot]} equipped`}
              >
                {itemId && <ItemIcon itemId={itemId} className="inventory-cell-icon" />}
                <span className="inventory-slot-caption">{SLOT_LABELS[slot]}</span>
              </button>
            )
          })}
        </div>
      </div>
    </Portal>
  )
}

function ItemDetailPanel({
  definition,
  canEquip,
  isEquipping,
  onEquip,
  error,
}: {
  definition: ItemDefinition | null
  canEquip: boolean
  isEquipping: boolean
  onEquip: () => void
  error: string | null
}) {
  return (
    <div className="inventory-detail-panel">
      {definition ? (
        <>
          <ItemDetailCard definition={definition} />
          {canEquip ? (
            <button type="button" className="btn btn-primary btn-block" onClick={onEquip} disabled={isEquipping}>
              {isEquipping ? 'Equipping…' : 'Equip'}
            </button>
          ) : (
            <p className="muted inventory-equipped-note">Currently equipped</p>
          )}
        </>
      ) : (
        <p className="muted">Tap an item below to see its details.</p>
      )}
      {error && (
        <p className="alert" role="alert">
          {error}
        </p>
      )}
    </div>
  )
}
