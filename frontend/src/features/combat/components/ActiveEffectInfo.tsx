import {Portal} from '../../../shared/ui/Portal'
import type {ActiveEffect} from '../types'

interface Props {
    effect: ActiveEffect
    onClose: () => void
}

export function ActiveEffectInfo({effect, onClose}: Props) {
    const turnsLabel = effect.remainingTurns === 1 ? '1 more turn' : `${effect.remainingTurns} more turns`

    return (
        <Portal>
            <div className="enemy-info-overlay" onClick={onClose}>
                <div className="enemy-info-card" onClick={(event) => event.stopPropagation()}>
                    <div className="ability-card-header">
                        <span className="ability-name">{effect.name}</span>
                        <button type="button" className="icon-btn" onClick={onClose} aria-label="Close">
                            ✕
                        </button>
                    </div>
                    <p className="ability-description">{effect.description}</p>
                    <p className="ability-meta">{turnsLabel}</p>
                </div>
            </div>
        </Portal>
    )
}
