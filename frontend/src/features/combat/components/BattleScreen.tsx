import { useEffect, useRef, useState } from 'react'
import { useCombatState } from '../useCombatState'
import { useClassDefinitions } from '../../classes/useClassDefinitions'
import { endTurn, endTurnAsFakeMember, useAbility, useAbilityAsFakeMember } from '../api'
import { CombatantCard, type FloatingText } from './CombatantCard'
import { AbilityActionPanel } from './AbilityActionPanel'
import { SelfStatsPanel } from './SelfStatsPanel'
import type { PartyMember } from '../../party/types'
import type { CombatState } from '../types'

interface Props {
  code: string
  members: PartyMember[]
  actingAsId: string
  selfUserId: string
  useDevActions?: boolean
}

const REACTION_DURATION_MS = 650
const ATTACK_DURATION_MS = 500
const FLOAT_DURATION_MS = 1100

export function BattleScreen({ code, members, actingAsId, selfUserId, useDevActions = false }: Props) {
  const { combat, error } = useCombatState(code)
  const { definitions } = useClassDefinitions()
  const [selectedAbilityId, setSelectedAbilityId] = useState<string | null>(null)
  const [hasActedThisTurn, setHasActedThisTurn] = useState(false)
  const [actionError, setActionError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  const [attackerId, setAttackerId] = useState<string | null>(null)
  const [reactions, setReactions] = useState<Record<string, 'hit' | 'healed'>>({})
  const [floatingByCombatant, setFloatingByCombatant] = useState<Record<string, FloatingText[]>>({})
  const previousCombatRef = useRef<CombatState | null>(null)
  const floatingKeyRef = useRef(0)

  useEffect(() => {
    setSelectedAbilityId(null)
    setHasActedThisTurn(false)
  }, [combat?.currentTurnCombatantId])

  useEffect(() => {
    previousCombatRef.current = null
  }, [code])

  // Diff the previous combat snapshot against the new one to figure out who
  // hit/healed whom, since the server only ever pushes full-state snapshots.
  // Damage/heals never cross the ally/enemy line except one way: player
  // abilities only ever damage the enemy or heal/buff allies, and the enemy's
  // one auto-attack only ever damages an ally — so whichever side got hurt
  // tells us the actor unambiguously. (We can't infer the actor from whether
  // currentTurnCombatantId changed: in a 1-ally party the turn sequence is
  // just [ally, enemy], so ending your turn cycles right back to you and the
  // id looks unchanged even though the enemy acted in between.)
  useEffect(() => {
    if (!combat) {
      return
    }
    const previous = previousCombatRef.current
    if (previous) {
      const hitTargets: string[] = []
      const healedTargets: string[] = []
      const newFloats: { combatantId: string; entry: FloatingText }[] = []

      for (const current of combat.combatants) {
        const before = previous.combatants.find((candidate) => candidate.id === current.id)
        if (!before) {
          continue
        }
        const delta = current.currentHealth - before.currentHealth
        if (delta === 0) {
          continue
        }
        const kind: 'damage' | 'heal' = delta < 0 ? 'damage' : 'heal'
        if (kind === 'damage') {
          hitTargets.push(current.id)
        } else {
          healedTargets.push(current.id)
        }
        newFloats.push({
          combatantId: current.id,
          entry: { key: `f${floatingKeyRef.current++}`, text: delta > 0 ? `+${delta}` : `${delta}`, kind },
        })
      }

      if (hitTargets.length > 0 || healedTargets.length > 0) {
        const damagedAllyId = combat.combatants.find(
          (combatant) => !combatant.enemy && hitTargets.includes(combatant.id),
        )?.id
        const actorId = damagedAllyId
          ? (combat.combatants.find((combatant) => combatant.enemy)?.id ?? null)
          : previous.currentTurnCombatantId
        if (actorId) {
          setAttackerId(actorId)
          setTimeout(() => setAttackerId((current) => (current === actorId ? null : current)), ATTACK_DURATION_MS)
        }
        setReactions((current) => {
          const next = { ...current }
          for (const id of hitTargets) next[id] = 'hit'
          for (const id of healedTargets) next[id] = 'healed'
          return next
        })
        setTimeout(() => {
          setReactions((current) => {
            const next = { ...current }
            for (const id of hitTargets) delete next[id]
            for (const id of healedTargets) delete next[id]
            return next
          })
        }, REACTION_DURATION_MS)

        setFloatingByCombatant((current) => {
          const next = { ...current }
          for (const { combatantId, entry } of newFloats) {
            next[combatantId] = [...(next[combatantId] ?? []), entry]
          }
          return next
        })
        for (const { combatantId, entry } of newFloats) {
          setTimeout(() => {
            setFloatingByCombatant((current) => ({
              ...current,
              [combatantId]: (current[combatantId] ?? []).filter((floating) => floating.key !== entry.key),
            }))
          }, FLOAT_DURATION_MS)
        }
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
  const isMyTurn = combat.currentTurnCombatantId === actingAsId
  const selfCombatant = combat.combatants.find((combatant) => combatant.id === actingAsId) ?? null
  const selfDefinition = selfCombatant?.characterClass
    ? (definitions.find((definition) => definition.characterClass === selfCombatant.characterClass) ?? null)
    : null
  const selectedAbility = selfDefinition?.abilities.find((ability) => ability.id === selectedAbilityId) ?? null

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
      if (!useDevActions && actingAsId === selfUserId) {
        await useAbility(code, abilityId, targetId)
      } else {
        await useAbilityAsFakeMember(code, actingAsId, abilityId, targetId)
      }
      setSelectedAbilityId(null)
      setHasActedThisTurn(true)
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
      if (!useDevActions && actingAsId === selfUserId) {
        await endTurn(code)
      } else {
        await endTurnAsFakeMember(code, actingAsId)
      }
      // Reset explicitly rather than relying only on the
      // combat?.currentTurnCombatantId effect above: in a 1-ally party the
      // turn sequence is [ally, enemy], so ending your turn can cycle
      // straight back to your own id, which looks like no change at all.
      setHasActedThisTurn(false)
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
              isEngaged={combat.currentTurnCombatantId === combatant.id && hasActedThisTurn}
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
                isEngaged={combat.currentTurnCombatantId === combatant.id && hasActedThisTurn}
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
          </section>
        )}

        {combat.status === 'IN_PROGRESS' && selfCombatant && selfDefinition && (
          <SelfStatsPanel
            self={selfCombatant}
            stats={selfDefinition.stats}
            isMyTurn={isMyTurn}
            hasSelectedAbility={selectedAbility != null}
            hasActedThisTurn={hasActedThisTurn}
            isSubmitting={isSubmitting}
            onEndTurn={handleEndTurn}
            onCancel={() => setSelectedAbilityId(null)}
          />
        )}

        {combat.status === 'IN_PROGRESS' && isMyTurn && selfCombatant && selfDefinition && (
          <AbilityActionPanel
            self={selfCombatant}
            abilities={selfDefinition.abilities}
            selectedAbility={selectedAbility}
            isSubmitting={isSubmitting}
            onSelectAbility={setSelectedAbilityId}
          />
        )}

        {combat.status === 'IN_PROGRESS' && !isMyTurn && (
          <p className="muted battle-waiting">
            Waiting for {combat.combatants.find((combatant) => combatant.id === combat.currentTurnCombatantId)?.displayName ?? '...'}
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
