import { useEffect, useRef, useState } from 'react'
import { useCombatState } from '../useCombatState'
import { useClassDefinitions } from '../../classes/useClassDefinitions'
import { endTurn, endTurnAsFakeMember, useAbility, useAbilityAsFakeMember } from '../api'
import { CombatantCard, type FloatingText } from './CombatantCard'
import { AbilityActionPanel } from './AbilityActionPanel'
import type { PartyMember } from '../../party/types'
import type { CombatState } from '../types'

interface Props {
  code: string
  members: PartyMember[]
  actingAsId: string
  selfUserId: string
}

const REACTION_DURATION_MS = 650
const ATTACK_DURATION_MS = 500
const FLOAT_DURATION_MS = 1100

export function BattleScreen({ code, members, actingAsId, selfUserId }: Props) {
  const { combat, error } = useCombatState(code)
  const { definitions } = useClassDefinitions()
  const [selectedAbilityId, setSelectedAbilityId] = useState<string | null>(null)
  const [hasActedThisTurn, setHasActedThisTurn] = useState(false)
  const [actionError, setActionError] = useState<string | null>(null)

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
  // The current-turn combatant is the actor when the turn hasn't advanced
  // (they used an ability); when it *has* advanced, any HP change is the
  // enemy's automatic attack (v1 has exactly one enemy, so this is unambiguous).
  useEffect(() => {
    if (!combat) {
      return
    }
    const previous = previousCombatRef.current
    if (previous) {
      const actorId =
        previous.currentTurnCombatantId === combat.currentTurnCombatantId
          ? previous.currentTurnCombatantId
          : (combat.combatants.find((combatant) => combatant.enemy)?.id ?? null)

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

  const needsFieldTarget =
    selectedAbility && (selectedAbility.targetType === 'SINGLE_ALLY' || selectedAbility.targetType === 'SINGLE_ENEMY')
  const selectableIds = needsFieldTarget
    ? new Set(
        combat.combatants
          .filter((combatant) => combatant.alive && combatant.enemy === (selectedAbility!.targetType === 'SINGLE_ENEMY'))
          .map((combatant) => combatant.id),
      )
    : null

  async function submitAbility(abilityId: string, targetId: string | null) {
    setActionError(null)
    try {
      if (actingAsId === selfUserId) {
        await useAbility(code, abilityId, targetId)
      } else {
        await useAbilityAsFakeMember(code, actingAsId, abilityId, targetId)
      }
      setSelectedAbilityId(null)
      setHasActedThisTurn(true)
    } catch {
      setActionError('That action failed. Try again.')
    }
  }

  function handleFieldTargetClick(targetId: string) {
    if (selectedAbility && selectableIds?.has(targetId)) {
      void submitAbility(selectedAbility.id, targetId)
    }
  }

  async function handleEndTurn() {
    setActionError(null)
    try {
      if (actingAsId === selfUserId) {
        await endTurn(code)
      } else {
        await endTurnAsFakeMember(code, actingAsId)
      }
    } catch {
      setActionError('Could not end turn. Try again.')
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
              selectable={selectableIds?.has(combatant.id) ?? false}
              onSelect={() => handleFieldTargetClick(combatant.id)}
              isAttacking={attackerId === combatant.id}
              reaction={reactions[combatant.id] ?? null}
              floatingTexts={floatingByCombatant[combatant.id]}
            />
          ))}
        </div>

        <div className="battlefield-divider" aria-hidden="true" />

        <div className="battlefield-row battlefield-row-allies">
          {allies.map((combatant) => {
            const member = members.find((candidate) => candidate.userId === combatant.id)
            return (
              <CombatantCard
                key={combatant.id}
                combatant={combatant}
                member={member}
                isCurrentTurn={combat.currentTurnCombatantId === combatant.id}
                selectable={selectableIds?.has(combatant.id) ?? false}
                onSelect={() => handleFieldTargetClick(combatant.id)}
                isAttacking={attackerId === combatant.id}
                reaction={reactions[combatant.id] ?? null}
                floatingTexts={floatingByCombatant[combatant.id]}
              />
            )
          })}
        </div>
      </div>

      {combat.status !== 'IN_PROGRESS' && (
        <section className="card battle-result">
          <h2 className="section-title">{combat.status === 'PARTY_WON' ? 'Victory!' : 'Defeat...'}</h2>
        </section>
      )}

      {combat.status === 'IN_PROGRESS' && isMyTurn && selfCombatant && selfDefinition && (
        <AbilityActionPanel
          self={selfCombatant}
          abilities={selfDefinition.abilities}
          selectedAbility={selectedAbility}
          hasActedThisTurn={hasActedThisTurn}
          onSelectAbility={setSelectedAbilityId}
          onUndo={() => setSelectedAbilityId(null)}
          onConfirm={(abilityId) => void submitAbility(abilityId, null)}
          onEndTurn={handleEndTurn}
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
  )
}
