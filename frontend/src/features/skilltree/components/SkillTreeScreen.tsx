import {useEffect, useMemo, useRef, useState} from 'react'
import {Portal} from '../../../shared/ui/Portal'
import {ManaIcon} from '../../../shared/ui/ManaIcon'
import {AbilityCard} from '../../classes/components/AbilityCard'
import {unlockSkill} from '../api'
import {useSkillTrees} from '../useSkillTrees'
import type {PartyMember, PartyState} from '../../party/types'
import type {SkillNode} from '../types'

interface Props {
    code: string
    member: PartyMember
    onApplyUpdate: (state: PartyState) => void
    onClose: () => void
}

interface ConnectorLine {
    key: string
    x1: number
    y1: number
    x2: number
    y2: number
}

export function SkillTreeScreen({code, member, onApplyUpdate, onClose}: Props) {
    const {trees, error: loadError} = useSkillTrees()
    const [selectedNodeId, setSelectedNodeId] = useState<string | null>(null)
    const [isUnlocking, setIsUnlocking] = useState(false)
    const [actionError, setActionError] = useState<string | null>(null)
    const [connectors, setConnectors] = useState<ConnectorLine[]>([])

    const containerRef = useRef<HTMLDivElement | null>(null)
    const nodeElements = useRef(new Map<string, HTMLButtonElement>())

    const tree = trees.find((candidate) => candidate.characterClass === member.characterClass) ?? null
    const unlockedSet = useMemo(() => new Set(member.unlockedSkillIds), [member.unlockedSkillIds])
    const selectedNode = tree?.nodes.find((node) => node.id === selectedNodeId) ?? null

    const childrenByParentId = useMemo(() => {
        const map = new Map<string, SkillNode[]>()
        if (!tree) {
            return map
        }
        for (const node of tree.nodes) {
            if (node.parentId === null) {
                continue
            }
            const siblings = map.get(node.parentId) ?? []
            siblings.push(node)
            map.set(node.parentId, siblings)
        }
        return map
    }, [tree])

    const roots = tree?.nodes.filter((node) => node.parentId === null) ?? []

    // Draws each parent->child connection as an actual curved branch line
    // between the two chips' real rendered positions, rather than a flat
    // CSS bar spanning a whole sibling row — measured directly since a pure
    // CSS approach can't know two chips' relative positions without it.
    // Recomputed whenever the tree data loads/changes and on resize; layout
    // effect (not a regular effect) so it runs before paint and this doesn't
    // flash unconnected chips first.
    useEffect(() => {
        function recompute() {
            const container = containerRef.current
            if (!container || !tree) {
                setConnectors([])
                return
            }
            const containerRect = container.getBoundingClientRect()
            const lines: ConnectorLine[] = []
            for (const node of tree.nodes) {
                if (node.parentId === null) {
                    continue
                }
                const childEl = nodeElements.current.get(node.id)
                const parentEl = nodeElements.current.get(node.parentId)
                if (!childEl || !parentEl) {
                    continue
                }
                const childRect = childEl.getBoundingClientRect()
                const parentRect = parentEl.getBoundingClientRect()
                lines.push({
                    key: node.id,
                    x1: parentRect.left + parentRect.width / 2 - containerRect.left,
                    y1: parentRect.bottom - containerRect.top,
                    x2: childRect.left + childRect.width / 2 - containerRect.left,
                    y2: childRect.top - containerRect.top,
                })
            }
            setConnectors(lines)
        }

        recompute()
        window.addEventListener('resize', recompute)
        return () => window.removeEventListener('resize', recompute)
    }, [tree])

    async function handleUnlock(node: SkillNode) {
        if (isUnlocking) {
            return
        }
        setActionError(null)
        setIsUnlocking(true)
        try {
            onApplyUpdate(await unlockSkill(code, member.userId, node.id))
        } catch {
            setActionError('Could not unlock that skill. Try again.')
        } finally {
            setIsUnlocking(false)
        }
    }

    function registerNode(nodeId: string, el: HTMLButtonElement | null) {
        if (el) {
            nodeElements.current.set(nodeId, el)
        } else {
            nodeElements.current.delete(nodeId)
        }
    }

    return (
        <Portal>
            <div className="skilltree-screen">
                <div className="skilltree-header">
                    <h2 className="section-title">Skill Tree</h2>
                    <button type="button" className="icon-btn" onClick={onClose} aria-label="Close skill tree">
                        ✕
                    </button>
                </div>

                <div className="skilltree-mana-banner">
                    <ManaIcon className="skilltree-mana-icon"/>
                    <span>{member.mana} mana</span>
                </div>

                <SkillNodeDetailPanel
                    node={selectedNode}
                    isUnlocked={selectedNode != null && unlockedSet.has(selectedNode.id)}
                    isPrerequisiteMet={
                        selectedNode != null && (selectedNode.parentId === null || unlockedSet.has(selectedNode.parentId))
                    }
                    mana={member.mana}
                    isUnlocking={isUnlocking}
                    onUnlock={handleUnlock}
                    error={actionError}
                />

                {loadError && (
                    <p className="alert" role="alert">
                        {loadError}
                    </p>
                )}

                {!tree && !loadError && <p className="muted">Loading skill tree...</p>}

                {tree && (
                    <div className="skilltree-scroll">
                        <div className="skilltree-container" ref={containerRef}>
                            <svg className="skilltree-connectors">
                                {connectors.map((line) => {
                                    const midY = (line.y1 + line.y2) / 2
                                    return (
                                        <path
                                            key={line.key}
                                            d={`M ${line.x1} ${line.y1} C ${line.x1} ${midY}, ${line.x2} ${midY}, ${line.x2} ${line.y2}`}
                                        />
                                    )
                                })}
                            </svg>
                            {roots.map((root) => (
                                <SkillNodeBranch
                                    key={root.id}
                                    node={root}
                                    childrenByParentId={childrenByParentId}
                                    unlockedSet={unlockedSet}
                                    selectedNodeId={selectedNodeId}
                                    onSelect={setSelectedNodeId}
                                    registerNode={registerNode}
                                />
                            ))}
                        </div>
                    </div>
                )}
            </div>
        </Portal>
    )
}

function SkillNodeDetailPanel({
                                  node,
                                  isUnlocked,
                                  isPrerequisiteMet,
                                  mana,
                                  isUnlocking,
                                  onUnlock,
                                  error,
                              }: {
    node: SkillNode | null
    isUnlocked: boolean
    isPrerequisiteMet: boolean
    mana: number
    isUnlocking: boolean
    onUnlock: (node: SkillNode) => void
    error: string | null
}) {
    if (!node) {
        return (
            <div className="skilltree-detail-panel">
                <p className="muted">Tap a node below to see its details.</p>
            </div>
        )
    }

    const canAfford = mana >= node.manaCost
    const isUnlockable = !isUnlocked && isPrerequisiteMet

    return (
        <div className="skilltree-detail-panel">
            <div className="skill-node-detail-header">
                <span className="skill-node-name">{node.name}</span>
                <span className="skill-node-cost">
                    <ManaIcon className="skill-node-cost-icon"/>
                    {node.manaCost}
                </span>
            </div>
            <p className="skill-node-description">{node.description}</p>
            <AbilityCard ability={node.resultingAbility}/>

            {isUnlocked && <p className="skill-node-status">Unlocked</p>}
            {!isUnlocked && !isPrerequisiteMet && (
                <p className="skill-node-status muted">Unlock its prerequisite first</p>
            )}
            {isUnlockable && (
                <button
                    type="button"
                    className="btn btn-primary btn-block"
                    disabled={!canAfford || isUnlocking}
                    onClick={() => onUnlock(node)}
                >
                    {isUnlocking ? 'Unlocking…' : canAfford ? 'Unlock' : 'Not enough mana'}
                </button>
            )}
            {error && (
                <p className="alert" role="alert">
                    {error}
                </p>
            )}
        </div>
    )
}

function SkillNodeBranch({
                             node,
                             childrenByParentId,
                             unlockedSet,
                             selectedNodeId,
                             onSelect,
                             registerNode,
                         }: {
    node: SkillNode
    childrenByParentId: Map<string, SkillNode[]>
    unlockedSet: Set<string>
    selectedNodeId: string | null
    onSelect: (nodeId: string) => void
    registerNode: (nodeId: string, el: HTMLButtonElement | null) => void
}) {
    const children = childrenByParentId.get(node.id) ?? []
    const isUnlocked = unlockedSet.has(node.id)
    const isPrerequisiteMet = node.parentId === null || unlockedSet.has(node.parentId)
    const isUnlockable = !isUnlocked && isPrerequisiteMet

    return (
        <div className="skill-node-branch">
            <button
                type="button"
                ref={(el) => registerNode(node.id, el)}
                className={[
                    'skill-node-chip',
                    isUnlocked ? 'is-unlocked' : isUnlockable ? 'is-unlockable' : 'is-locked',
                    selectedNodeId === node.id ? 'is-selected' : '',
                ].join(' ')}
                onClick={() => onSelect(node.id)}
            >
                <span className="skill-node-chip-name">{node.name}</span>
                <span className="skill-node-chip-cost">
                    <ManaIcon className="skill-node-chip-cost-icon"/>
                    {node.manaCost}
                </span>
            </button>

            {children.length > 0 && (
                <div className="skill-node-children">
                    {children.map((child) => (
                        <SkillNodeBranch
                            key={child.id}
                            node={child}
                            childrenByParentId={childrenByParentId}
                            unlockedSet={unlockedSet}
                            selectedNodeId={selectedNodeId}
                            onSelect={onSelect}
                            registerNode={registerNode}
                        />
                    ))}
                </div>
            )}
        </div>
    )
}
