import {useEffect, useRef, useState} from 'react'
import {Link} from 'react-router-dom'
import {useCombatState} from '../useCombatState'
import {useClassDefinitions} from '../../classes/useClassDefinitions'
import {endTurn, useAbility} from '../api'
import {CombatantCard, type FloatingText} from './CombatantCard'
import {AbilityActionPanel} from './AbilityActionPanel'
import {SelfStatsPanel} from './SelfStatsPanel'
import {EquippedItemsPanel} from './EquippedItemsPanel'
import type {PartyMember} from '../../party/types'
import type {CombatState} from '../types'

interface Props {
    code: string
    members: PartyMember[]
    actingAsId: string
}

const REACTION_DURATION_MS = 650
const ATTACK_DURATION_MS = 500
const FLOAT_DURATION_MS = 1100
const FLOAT_SPREAD_PX = 22
const FLOAT_STAGGER_MS = 90

interface TopContributor {
    displayName: string
    amount: number
}

// Whoever's own running total (accumulated server-side across the whole
// fight, not just the most recent action) is highest wins the spot — null if
// nobody on either side ever actually dealt/healed a positive amount, so a
// short fight with no healer, say, just omits that row instead of crowning
// someone at 0.
function topContributor(
    combatants: CombatState['combatants'],
    stat: 'totalDamageDealt' | 'totalHealingDone' | 'totalEffectsApplied',
): TopContributor | null {
    return combatants.reduce<TopContributor | null>((best, combatant) => {
        const amount = combatant[stat]
        if (amount <= 0 || (best && amount <= best.amount)) {
            return best
        }
        return {displayName: combatant.displayName, amount}
    }, null)
}

export function BattleScreen({code, members, actingAsId}: Props) {
    const {combat, error, applyUpdate} = useCombatState(code)
    const {definitions} = useClassDefinitions()
    const [selectedAbilityId, setSelectedAbilityId] = useState<string | null>(null)
    const [actionError, setActionError] = useState<string | null>(null)
    const [isSubmitting, setIsSubmitting] = useState(false)

    const [attackerId, setAttackerId] = useState<string | null>(null)
    const [reactions, setReactions] = useState<Record<string, 'hit' | 'healed'>>({})
    const [floatingByCombatant, setFloatingByCombatant] = useState<Record<string, FloatingText[]>>({})
    const previousCombatRef = useRef<CombatState | null>(null)
    const floatingKeyRef = useRef(0)

    useEffect(() => {
        setSelectedAbilityId(null)
    }, [combat?.currentTurnCombatantId])

    useEffect(() => {
        previousCombatRef.current = null
    }, [code])

    // Diff the previous combat snapshot against the new one to figure out who
    // hit/healed whom, since the server only ever pushes full-state snapshots.
    // The backend broadcasts once per individual actor's action — including
    // once per enemy within a multi-enemy end-turn cascade, each paced apart
    // (see CombatService.endTurn) — so whoever's currentTurnCombatantId is an
    // enemy right now is reliably the one who just acted; the server pauses
    // right on them for exactly this reason. Otherwise (a player's own
    // ability use, or an ally's turn just beginning) currentTurnCombatantId
    // doesn't change while they're still mid-turn, so previous still
    // correctly points at them.
    useEffect(() => {
        if (!combat) {
            return
        }
        const previous = previousCombatRef.current
        if (previous) {
            const hitTargets: string[] = []
            const healedTargets: string[] = []

            for (const current of combat.combatants) {
                const before = previous.combatants.find((candidate) => candidate.id === current.id)
                if (!before) {
                    continue
                }
                const delta = current.currentHealth - before.currentHealth
                if (delta < 0) {
                    hitTargets.push(current.id)
                } else if (delta > 0) {
                    healedTargets.push(current.id)
                }
            }

            if (hitTargets.length > 0 || healedTargets.length > 0) {
                const currentActor = combat.combatants.find(
                    (combatant) => combatant.id === combat.currentTurnCombatantId,
                )
                const actorId = currentActor?.enemy ? currentActor.id : previous.currentTurnCombatantId
                if (actorId) {
                    setAttackerId(actorId)
                    setTimeout(() => setAttackerId((current) => (current === actorId ? null : current)), ATTACK_DURATION_MS)
                }
                setReactions((current) => {
                    const next = {...current}
                    for (const id of hitTargets) next[id] = 'hit'
                    for (const id of healedTargets) next[id] = 'healed'
                    return next
                })
                setTimeout(() => {
                    setReactions((current) => {
                        const next = {...current}
                        for (const id of hitTargets) delete next[id]
                        for (const id of healedTargets) delete next[id]
                        return next
                    })
                }, REACTION_DURATION_MS)
            }

            // One floating-text popup per individual event (not one per net health
            // delta), so a multi-hit ability shows a popup per hit instead of a
            // single combined number. Events on the same target get a random
            // horizontal offset and a staggered start so repeated attacks (e.g.
            // Blade Flurry's 4 hits) don't land in the same fixed pattern every
            // time or render as one illegible stack.
            const eventsByTarget = new Map<string, typeof combat.recentEvents>()
            for (const event of combat.recentEvents) {
                const list = eventsByTarget.get(event.targetId) ?? []
                list.push(event)
                eventsByTarget.set(event.targetId, list)
            }

            for (const [combatantId, events] of eventsByTarget) {
                events.forEach((event, index) => {
                    const kind: 'damage' | 'heal' = event.kind === 'HEAL' ? 'heal' : 'damage'
                    const entry: FloatingText = {
                        key: `f${floatingKeyRef.current++}`,
                        text: kind === 'heal' ? `+${event.amount}` : `-${event.amount}${event.critical ? '!' : ''}`,
                        kind,
                        critical: event.critical,
                        offsetX: (Math.random() - 0.5) * 2 * FLOAT_SPREAD_PX,
                        delayMs: index * FLOAT_STAGGER_MS + Math.random() * FLOAT_STAGGER_MS * 0.6,
                    }
                    setFloatingByCombatant((current) => ({
                        ...current,
                        [combatantId]: [...(current[combatantId] ?? []), entry],
                    }))
                    setTimeout(
                        () => {
                            setFloatingByCombatant((current) => ({
                                ...current,
                                [combatantId]: (current[combatantId] ?? []).filter((floating) => floating.key !== entry.key),
                            }))
                        },
                        FLOAT_DURATION_MS + entry.delayMs,
                    )
                })
            }
        }
        previousCombatRef.current = combat
    }, [combat])

    if (error) {
        return (
            <p className="alert" role="alert">
                {error}
            </p>
        )
    }
    if (!combat) {
        return <p className="muted">Loading battle...</p>
    }

    const enemies = combat.combatants.filter((combatant) => combatant.enemy)
    const allies = combat.combatants.filter((combatant) => !combatant.enemy)
    const topDamageDealer = topContributor(combat.combatants, 'totalDamageDealt')
    const topHealer = topContributor(combat.combatants, 'totalHealingDone')
    const topEffectsApplier = topContributor(combat.combatants, 'totalEffectsApplied')
    const isMyTurn = combat.currentTurnCombatantId === actingAsId
    const selfCombatant = combat.combatants.find((combatant) => combatant.id === actingAsId) ?? null
    const selfMember = members.find((member) => member.userId === actingAsId) ?? null
    const selfDefinition = selfCombatant?.characterClass
        ? (definitions.find((definition) => definition.characterClass === selfCombatant.characterClass) ?? null)
        : null
    // Reads from the live combatant's own ability list (reflects unlocked
    // skill tree upgrades), not the static per-class catalog — selfDefinition
    // is still used below for base stats, which the skill tree never changes.
    const selectedAbility = selfCombatant?.abilities.find((ability) => ability.id === selectedAbilityId) ?? null

    // Every ability, single-target or area, is confirmed by tapping a
    // highlighted combatant — for an area ability any valid target works
    // since the server resolves the whole side regardless of which one was
    // tapped, same as it ignores the target for SELF.
    const selectableIds = selectedAbility
        ? new Set(
            selectedAbility.targetType === 'SELF'
                ? [actingAsId]
                : combat.combatants
                    .filter((combatant) => {
                        const targetsEnemies =
                            selectedAbility.targetType === 'SINGLE_ENEMY' || selectedAbility.targetType === 'ALL_ENEMIES'
                        return combatant.alive && combatant.enemy === targetsEnemies
                    })
                    .map((combatant) => combatant.id),
        )
        : null

    async function submitAbility(abilityId: string, targetId: string | null) {
        if (isSubmitting) {
            return
        }
        setActionError(null)
        setIsSubmitting(true)
        try {
            // Apply the response immediately rather than waiting for the
            // broadcast round-trip back over the WebSocket — it still arrives
            // moments later but is a no-op then, since it's identical to what we
            // just applied.
            applyUpdate(await useAbility(code, actingAsId, abilityId, targetId))
            setSelectedAbilityId(null)
        } catch {
            setActionError('That action failed. Try again.')
        } finally {
            setIsSubmitting(false)
        }
    }

    function handleFieldTargetClick(targetId: string) {
        if (!isSubmitting && selectedAbility && selectableIds?.has(targetId)) {
            void submitAbility(selectedAbility.id, targetId)
        }
    }

    async function handleEndTurn() {
        if (isSubmitting) {
            return
        }
        setActionError(null)
        setIsSubmitting(true)
        try {
            // Apply the response immediately rather than waiting for the
            // broadcast round-trip back over the WebSocket — it still arrives
            // moments later but is a no-op then, since it's identical to what we
            // just applied.
            applyUpdate(await endTurn(code, actingAsId))
            setSelectedAbilityId(null)
        } catch {
            setActionError('Could not end turn. Try again.')
        } finally {
            setIsSubmitting(false)
        }
    }

    return (
        <div className="battle-screen">
            <div className="battlefield">
                <div className="battlefield-row battlefield-row-enemies">
                    {enemies.map((combatant) => (
                        <CombatantCard
                            key={combatant.id}
                            combatant={combatant}
                            isCurrentTurn={combat.currentTurnCombatantId === combatant.id}
                            isEngaged={combat.currentTurnCombatantId === combatant.id && combatant.actedThisTurn}
                            selectable={selectableIds?.has(combatant.id) ?? false}
                            isInvalidTarget={selectableIds != null && !selectableIds.has(combatant.id) && combatant.alive}
                            onSelect={() => handleFieldTargetClick(combatant.id)}
                            isAttacking={attackerId === combatant.id}
                            reaction={reactions[combatant.id] ?? null}
                            floatingTexts={floatingByCombatant[combatant.id]}
                        />
                    ))}
                </div>

                <div className="battlefield-row battlefield-row-allies">
                    {allies.map((combatant) => {
                        const member = members.find((candidate) => candidate.userId === combatant.id)
                        return (
                            <CombatantCard
                                key={combatant.id}
                                combatant={combatant}
                                member={member}
                                isCurrentTurn={combat.currentTurnCombatantId === combatant.id}
                                isEngaged={combat.currentTurnCombatantId === combatant.id && combatant.actedThisTurn}
                                selectable={selectableIds?.has(combatant.id) ?? false}
                                isInvalidTarget={selectableIds != null && !selectableIds.has(combatant.id) && combatant.alive}
                                onSelect={() => handleFieldTargetClick(combatant.id)}
                                isAttacking={attackerId === combatant.id}
                                reaction={reactions[combatant.id] ?? null}
                                floatingTexts={floatingByCombatant[combatant.id]}
                            />
                        )
                    })}
                </div>
            </div>

            <div className="battle-controls">
                {combat.status !== 'IN_PROGRESS' && (
                    <section className="battle-result">
                        <h2 className="section-title">{combat.status === 'PARTY_WON' ? 'Victory!' : 'Defeat...'}</h2>

                        <div className="battle-stats-summary">
                            {topDamageDealer && (
                                <p className="battle-stat-row">
                                    <span className="battle-stat-label">Most damage dealt</span>
                                    <span className="battle-stat-value">
                                        {topDamageDealer.displayName} — {topDamageDealer.amount}
                                    </span>
                                </p>
                            )}
                            {topHealer && (
                                <p className="battle-stat-row">
                                    <span className="battle-stat-label">Most healing done</span>
                                    <span className="battle-stat-value">
                                        {topHealer.displayName} — {topHealer.amount}
                                    </span>
                                </p>
                            )}
                            {topEffectsApplier && (
                                <p className="battle-stat-row">
                                    <span className="battle-stat-label">Most effects applied</span>
                                    <span className="battle-stat-value">
                                        {topEffectsApplier.displayName} — {topEffectsApplier.amount}
                                    </span>
                                </p>
                            )}
                        </div>

                        {combat.status === 'PARTY_LOST' && (
                            <Link to="/" className="btn btn-primary btn-block">
                                Return to Lobby
                            </Link>
                        )}
                    </section>
                )}

                {combat.status === 'IN_PROGRESS' && selfCombatant && selfDefinition && (
                    <SelfStatsPanel
                        self={selfCombatant}
                        stats={selfDefinition.stats}
                        isMyTurn={isMyTurn}
                        hasSelectedAbility={selectedAbility != null}
                        isSubmitting={isSubmitting}
                        onEndTurn={handleEndTurn}
                        onCancel={() => setSelectedAbilityId(null)}
                    />
                )}

                {combat.status === 'IN_PROGRESS' && selfMember && <EquippedItemsPanel loadout={selfMember.loadout}/>}

                {combat.status === 'IN_PROGRESS' && isMyTurn && selfCombatant && selfDefinition && (
                    <AbilityActionPanel
                        self={selfCombatant}
                        abilities={selfCombatant.abilities}
                        selectedAbility={selectedAbility}
                        isSubmitting={isSubmitting}
                        onSelectAbility={setSelectedAbilityId}
                    />
                )}

                {combat.status === 'IN_PROGRESS' && !isMyTurn && (
                    <p className="muted battle-waiting">
                        Waiting
                        for {combat.combatants.find((combatant) => combatant.id === combat.currentTurnCombatantId)?.displayName ?? '...'}
                        ...
                    </p>
                )}

                {actionError && (
                    <p className="alert" role="alert">
                        {actionError}
                    </p>
                )}
            </div>
        </div>
    )
}
