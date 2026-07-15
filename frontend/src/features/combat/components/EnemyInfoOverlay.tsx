import {Portal} from '../../../shared/ui/Portal'
import {StatBar} from '../../classes/components/StatBar'
import type {Combatant} from '../types'

interface Props {
    combatant: Combatant
    onClose: () => void
}

export function EnemyInfoOverlay({combatant, onClose}: Props) {
    if (!combatant.attackName || !combatant.attackEffectSummary) {
        return null
    }

    return (
        <Portal>
            <div className="enemy-info-overlay" onClick={onClose}>
                <div className="enemy-info-card" onClick={(event) => event.stopPropagation()}>
                    <div className="ability-card-header">
                        <span className="ability-name">{combatant.displayName}</span>
                        <button type="button" className="icon-btn" onClick={onClose} aria-label="Close">
                            ✕
                        </button>
                    </div>

                    <div className="stat-bar-list">
                        <StatBar label="Health" value={combatant.maxHealth} max={300} theme="enemy"/>
                        <StatBar label="Defense" value={combatant.defense} max={30} theme="enemy"/>
                    </div>

                    <div className="ability-list">
                        <div className="ability-card">
                            <div className="ability-card-header">
                                <span className="ability-name">{combatant.attackName}</span>
                            </div>
                            <p className="ability-description">{combatant.attackDescription}</p>
                            <p className="ability-meta">{combatant.attackEffectSummary}</p>
                        </div>
                    </div>
                </div>
            </div>
        </Portal>
    )
}
