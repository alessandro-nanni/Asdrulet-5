import {Portal} from '../../../shared/ui/Portal'
import {ClassDetailsPanel} from '../../classes/components/ClassDetailsPanel'
import type {ClassDefinition} from '../../classes/types'

interface Props {
    definition: ClassDefinition
    onClose: () => void
}

export function ClassInfoOverlay({definition, onClose}: Props) {
    return (
        <Portal>
            <div className="enemy-info-overlay" onClick={onClose}>
                <div className="enemy-info-card" onClick={(event) => event.stopPropagation()}>
                    <div className="ability-card-header">
                        <span className="ability-name">{definition.displayName}</span>
                        <button type="button" className="icon-btn" onClick={onClose} aria-label="Close">
                            ✕
                        </button>
                    </div>
                    <ClassDetailsPanel definition={definition}/>
                </div>
            </div>
        </Portal>
    )
}
