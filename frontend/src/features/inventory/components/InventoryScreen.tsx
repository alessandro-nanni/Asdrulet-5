import {useMemo, useState} from 'react'
import {Portal} from '../../../shared/ui/Portal'
import {consumeItem, equipFromStorage} from '../../party/api'
import {useItemDefinitions} from '../useItemDefinitions'
import {ItemIcon} from './ItemIcon'
import {ItemDetailCard} from './ItemDetailCard'
import type {ItemDefinition, ItemSlot} from '../types'
import {SLOT_LABELS} from '../types'
import type {Loadout, PartyMember, PartyState} from '../../party/types'

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

// The equipped row only ever has a cell per equippable slot — CONSUMABLE
// items never leave shared storage, so they're excluded here (see canConsumeSelected/canEquipSelected below).
type EquippableSlot = Exclude<ItemSlot, 'CONSUMABLE'>

const SLOT_ORDER: EquippableSlot[] = ['WEAPON', 'CHESTPLATE', 'TRINKET']

const LOADOUT_FIELD: Record<EquippableSlot, keyof Loadout> = {
    WEAPON: 'weaponItemId',
    CHESTPLATE: 'chestplateItemId',
    TRINKET: 'trinketItemId',
}

export function InventoryScreen({code, member, storage, onApplyUpdate, onClose}: Props) {
    const {definitions} = useItemDefinitions()
    const [selected, setSelected] = useState<Selection | null>(null)
    const [isEquipping, setIsEquipping] = useState(false)
    const [isConsuming, setIsConsuming] = useState(false)
    const [error, setError] = useState<string | null>(null)

    const definitionsById = useMemo(
        () => new Map(definitions.map((definition) => [definition.id, definition])),
        [definitions],
    )
    const selectedDefinition = selected ? (definitionsById.get(selected.itemId) ?? null) : null
    const selectedFromStorage = selected != null && selected.storageIndex !== null
    const canConsumeSelected = selectedFromStorage && selectedDefinition?.slot === 'CONSUMABLE'
    const canEquipSelected = selectedFromStorage && selectedDefinition?.slot !== 'CONSUMABLE'

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
            setSelected({itemId: selected.itemId, storageIndex: null})
        } catch {
            setError('Could not equip that item. Try again.')
        } finally {
            setIsEquipping(false)
        }
    }

    async function handleConsume() {
        if (!selected || selected.storageIndex === null || isConsuming) {
            return
        }
        setError(null)
        setIsConsuming(true)
        try {
            onApplyUpdate(await consumeItem(code, member.userId, selected.storageIndex))
            // The item is gone — nothing left to keep selected.
            setSelected(null)
        } catch {
            setError('Could not consume that item. Try again.')
        } finally {
            setIsConsuming(false)
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
                    canConsume={canConsumeSelected}
                    isConsuming={isConsuming}
                    onConsume={handleConsume}
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
                                        setSelected({itemId, storageIndex: index})
                                        setError(null)
                                    }
                                }}
                                aria-label={itemId ? (definitionsById.get(itemId)?.displayName ?? itemId) : 'Empty storage cell'}
                            >
                                {itemId && <ItemIcon itemId={itemId} className="inventory-cell-icon"/>}
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
                                        setSelected({itemId, storageIndex: null})
                                        setError(null)
                                    }
                                }}
                                aria-label={itemId ? (definitionsById.get(itemId)?.displayName ?? itemId) : `No ${SLOT_LABELS[slot]} equipped`}
                            >
                                {itemId && <ItemIcon itemId={itemId} className="inventory-cell-icon"/>}
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
                             canConsume,
                             isConsuming,
                             onConsume,
                             error,
                         }: {
    definition: ItemDefinition | null
    canEquip: boolean
    isEquipping: boolean
    onEquip: () => void
    canConsume: boolean
    isConsuming: boolean
    onConsume: () => void
    error: string | null
}) {
    return (
        <div className="inventory-detail-panel">
            {definition ? (
                <>
                    <ItemDetailCard definition={definition}/>
                    {canConsume ? (
                        <button type="button" className="btn btn-primary btn-block" onClick={onConsume}
                                disabled={isConsuming}>
                            {isConsuming ? 'Consuming…' : 'Consume'}
                        </button>
                    ) : canEquip ? (
                        <button type="button" className="btn btn-primary btn-block" onClick={onEquip}
                                disabled={isEquipping}>
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
