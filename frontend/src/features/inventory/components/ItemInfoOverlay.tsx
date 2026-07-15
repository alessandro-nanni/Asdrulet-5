import {Portal} from '../../../shared/ui/Portal'
import {ItemDetailCard} from './ItemDetailCard'
import type {ItemDefinition} from '../types'

interface Props {
    definition: ItemDefinition
    onClose: () => void
}

export function ItemInfoOverlay({definition, onClose}: Props) {
    return (
        <Portal>
            <div className="enemy-info-overlay" onClick={onClose}>
                <div className="enemy-info-card ability-info-card" onClick={(event) => event.stopPropagation()}>
                    <button type="button" className="icon-btn ability-info-close" onClick={onClose} aria-label="Close">
                        ✕
                    </button>
                    <ItemDetailCard definition={definition}/>
                </div>
            </div>
        </Portal>
    )
}
