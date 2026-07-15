import type {Ability} from '../types'

const TARGET_LABELS: Record<Ability['targetType'], string> = {
    SELF: 'Self',
    SINGLE_ALLY: 'Single ally',
    ALL_ALLIES: 'All allies',
    SINGLE_ENEMY: 'Single enemy',
    ALL_ENEMIES: 'All enemies',
}

export function AbilityCard({ability}: { ability: Ability }) {
    const isUltimate = ability.type === 'ULTIMATE'
    const costLabel = isUltimate
        ? `Charges from damage dealt · ${ability.chargeThreshold} to unleash`
        : `${ability.staminaCost} stamina`

    return (
        <div className={`ability-card ${isUltimate ? 'is-ultimate' : ''}`}>
            <div className="ability-card-header">
                <span className="ability-name">{ability.name}</span>
                {isUltimate && <span className="badge badge-ultimate">Ultimate</span>}
            </div>
            <p className="ability-description">{ability.description}</p>
            <p className="ability-meta">
                {ability.effectSummary} · {costLabel} · {TARGET_LABELS[ability.targetType]}
            </p>
        </div>
    )
}
