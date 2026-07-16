import {useEffect, useState} from 'react'
import {Portal} from '../../../shared/ui/Portal'
import {CoinIcon} from '../../../shared/ui/CoinIcon'
import {acknowledgeLootResult, lootChest} from '../../party/api'
import {useItemDefinitions} from '../../inventory/useItemDefinitions'
import {ItemIcon} from '../../inventory/components/ItemIcon'
import roomLoot from '../../../assets/dungeon/room-loot.png'
import type {LootResult, PartyMember, PartyState} from '../../party/types'

interface Props {
    code: string
    selfId: string
    members: PartyMember[]
    lootResults: Record<string, LootResult>
    turnOrder: string[]
    onApplyUpdate: (state: PartyState) => void
}

const OPEN_DURATION_MS = 900
// How long the result stays on screen, announced, before this client tells
// the server it's done looking — see acknowledgeLootResult. Not a backend
// timer: the room only actually clears once every member's own client has
// paused like this and confirmed, mirroring the mystery wheel's own flow.
const ANNOUNCE_PAUSE_MS = 3000

export function LootRoomScreen({code, selfId, members, lootResults, turnOrder, onApplyUpdate}: Props) {
    const {definitions} = useItemDefinitions()
    const [isOpening, setIsOpening] = useState(false)
    const [error, setError] = useState<string | null>(null)

    const definitionsById = new Map(definitions.map((definition) => [definition.id, definition]))
    // Derived straight from the current lootResults prop, not tracked as its
    // own state — the dungeon-entry broadcast (which mounts this screen) and
    // the party-state broadcast (which carries a freshly-reset lootResults
    // for the new room) travel on separate WebSocket topics with no
    // ordering guarantee, so a useState seeded once at mount could freeze in
    // whichever one happened to arrive first, including a stale "already
    // looted" from the previous chest.
    const hasLooted = selfId in lootResults
    const selfResult = lootResults[selfId] ?? null
    const showResult = hasLooted && !isOpening && selfResult != null
    const nextToLootId = turnOrder.find((id) => !(id in lootResults)) ?? null
    const isMyTurn = nextToLootId === selfId
    const nextToLootMember = members.find((member) => member.userId === nextToLootId) ?? null
    const everyoneHasLooted = members.every((member) => member.userId in lootResults)

    function itemLabel(itemId: string): string {
        return definitionsById.get(itemId)?.displayName ?? itemId
    }

    function summarize(result: LootResult): string {
        const parts: string[] = []
        if (result.coins > 0) parts.push(`${result.coins} coins`)
        result.itemIds.forEach((itemId) => parts.push(itemLabel(itemId)))
        return parts.join(' + ')
    }

    // Fires once the result is actually visible — whether that's right away
    // (rejoining a room where this member already looted) or after the
    // opening animation finishes — and tells the server this client is done
    // looking, after a brief pause.
    useEffect(() => {
        if (!showResult) {
            return
        }
        const timer = setTimeout(() => {
            acknowledgeLootResult(code, selfId).catch(() => {
            })
        }, ANNOUNCE_PAUSE_MS)
        return () => clearTimeout(timer)
    }, [showResult, code, selfId])

    async function handleOpen() {
        if (isOpening || hasLooted || !isMyTurn) {
            return
        }
        setError(null)
        setIsOpening(true)
        try {
            const updated = await lootChest(code, selfId)
            onApplyUpdate(updated)
            setTimeout(() => setIsOpening(false), OPEN_DURATION_MS)
        } catch {
            setError('Could not open the chest. Try again.')
            setIsOpening(false)
        }
    }

    function subtitle(): string {
        if (showResult) {
            return everyoneHasLooted ? 'Everyone has looted — resolving...' : 'Waiting for the rest of the party to loot...'
        }
        if (isOpening) {
            return 'Opening…'
        }
        if (isMyTurn) {
            return "It's your turn — open the chest!"
        }
        if (nextToLootMember) {
            return `Waiting for ${nextToLootMember.displayName} to loot...`
        }
        return 'Everyone has looted.'
    }

    return (
        <Portal>
            <div className="loot-screen">
                <h2 className="section-title">Loot Room</h2>
                <p className="muted">{subtitle()}</p>

                <div className="loot-chest-shell">
                    <img src={roomLoot} alt="" className={`loot-chest-image${isOpening ? ' is-opening' : ''}`}/>
                </div>

                {!hasLooted && isMyTurn && (
                    <button type="button" className="btn btn-primary btn-block" onClick={handleOpen}
                            disabled={isOpening}>
                        {isOpening ? 'Opening…' : 'Open the chest'}
                    </button>
                )}

                {showResult && selfResult && (
                    <div className="loot-result">
                        <p className="loot-result-title">You found:</p>
                        <div className="loot-result-items">
                            {selfResult.coins > 0 && (
                                <span className="loot-result-pill">
                  <CoinIcon className="loot-result-icon"/>
                                    {selfResult.coins} coins
                </span>
                            )}
                            {selfResult.itemIds.map((itemId) => (
                                <span key={itemId} className="loot-result-pill">
                  <ItemIcon itemId={itemId} className="loot-result-item-icon"/>
                                    {itemLabel(itemId)}
                </span>
                            ))}
                        </div>
                    </div>
                )}

                {error && (
                    <p className="alert" role="alert">
                        {error}
                    </p>
                )}

                <ul className="loot-member-list">
                    {members.map((member) => {
                        const isSelf = member.userId === selfId
                        // Self's own find is deliberately held back until showResult
                        // (i.e. until the opening animation has actually finished) —
                        // otherwise it leaks into this list the instant the server
                        // responds, well before the chest visibly finishes opening.
                        // Nothing else drives another member's own opening animation
                        // from here, so their row just reflects the server the moment
                        // it's recorded.
                        const revealed = isSelf ? showResult : member.userId in lootResults
                        const result = revealed ? lootResults[member.userId] : undefined
                        const isNext = member.userId === nextToLootId
                        const label = result
                            ? summarize(result)
                            : isSelf && isOpening
                                ? 'Opening…'
                                : isNext
                                    ? 'Looting now…'
                                    : 'Waiting to loot…'
                        return (
                            <li key={member.userId} className={`loot-member-row${isNext ? ' is-current-turn' : ''}`}>
                                <span>{member.displayName}</span>
                                <span className="muted">{label}</span>
                            </li>
                        )
                    })}
                </ul>
            </div>
        </Portal>
    )
}
