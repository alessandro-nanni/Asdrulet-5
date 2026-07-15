import {useState} from 'react'
import {Portal} from '../../../shared/ui/Portal'
import {CoinIcon} from '../../../shared/ui/CoinIcon'
import {buyShopItem, leaveShop} from '../../party/api'
import {useItemDefinitions} from '../../inventory/useItemDefinitions'
import {ItemIcon} from '../../inventory/components/ItemIcon'
import {ItemDetailCard} from '../../inventory/components/ItemDetailCard'
import type {PartyState} from '../../party/types'

interface Props {
    code: string
    selfId: string
    isLeader: boolean
    coins: number
    shopStock: string[]
    onApplyUpdate: (state: PartyState) => void
}

export function ShopScreen({code, selfId, isLeader, coins, shopStock, onApplyUpdate}: Props) {
    const {definitions} = useItemDefinitions()
    const [selectedItemId, setSelectedItemId] = useState<string | null>(null)
    const [isBuying, setIsBuying] = useState(false)
    const [isLeaving, setIsLeaving] = useState(false)
    const [error, setError] = useState<string | null>(null)

    const definitionsById = new Map(definitions.map((definition) => [definition.id, definition]))
    const selectedDefinition = selectedItemId ? (definitionsById.get(selectedItemId) ?? null) : null

    async function handleBuy(itemId: string) {
        if (isBuying) {
            return
        }
        setError(null)
        setIsBuying(true)
        try {
            onApplyUpdate(await buyShopItem(code, selfId, itemId))
        } catch {
            setError('Could not buy that item. Try again.')
        } finally {
            setIsBuying(false)
        }
    }

    async function handleLeave() {
        if (isLeaving) {
            return
        }
        setIsLeaving(true)
        try {
            onApplyUpdate(await leaveShop(code, selfId))
        } catch {
            setError('Could not leave the shop. Try again.')
            setIsLeaving(false)
        }
    }

    return (
        <Portal>
            <div className="shop-screen">
                <div className="shop-header">
                    <h2 className="section-title">Merchant</h2>
                    <div className="shop-balance" aria-label={`${coins} coins`}>
                        <CoinIcon className="shop-balance-icon"/>
                        <span>{coins}</span>
                    </div>
                </div>

                {selectedDefinition && (
                    <div className="shop-detail-panel">
                        <ItemDetailCard definition={selectedDefinition}/>
                    </div>
                )}

                <ul className="shop-item-list">
                    {shopStock.map((itemId) => {
                        const definition = definitionsById.get(itemId)
                        if (!definition) {
                            return null
                        }
                        const canAfford = coins >= definition.price
                        return (
                            <li key={itemId} className="shop-item-row">
                                <button
                                    type="button"
                                    className="shop-item-info"
                                    onClick={() => setSelectedItemId(itemId)}
                                >
                                    <ItemIcon itemId={itemId} className="shop-item-icon"/>
                                    <span className="shop-item-name">{definition.displayName}</span>
                                </button>
                                {isLeader ? (
                                    <button
                                        type="button"
                                        className="btn btn-primary shop-item-buy-btn"
                                        onClick={() => handleBuy(itemId)}
                                        disabled={!canAfford || isBuying}
                                    >
                                        <CoinIcon className="shop-item-buy-icon"/>
                                        {definition.price}
                                    </button>
                                ) : (
                                    <span className="shop-item-price">
                    <CoinIcon className="shop-item-buy-icon"/>
                                        {definition.price}
                  </span>
                                )}
                            </li>
                        )
                    })}
                    {shopStock.length === 0 && <p className="muted">Sold out — nothing left to buy.</p>}
                </ul>

                {error && (
                    <p className="alert" role="alert">
                        {error}
                    </p>
                )}

                {isLeader ? (
                    <button type="button" className="btn btn-secondary btn-block" onClick={handleLeave}
                            disabled={isLeaving}>
                        Leave shop
                    </button>
                ) : (
                    <p className="muted">Only the leader can buy items and leave the shop.</p>
                )}
            </div>
        </Portal>
    )
}
