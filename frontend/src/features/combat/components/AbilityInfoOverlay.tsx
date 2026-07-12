import { Portal } from '../../../shared/ui/Portal'
import { AbilityCard } from '../../classes/components/AbilityCard'
import type { Ability } from '../../classes/types'

interface Props {
  ability: Ability
  onClose: () => void
}

export function AbilityInfoOverlay({ ability, onClose }: Props) {
  return (
    <Portal>
      <div className="enemy-info-overlay" onClick={onClose}>
        <div className="enemy-info-card ability-info-card" onClick={(event) => event.stopPropagation()}>
          <button type="button" className="icon-btn ability-info-close" onClick={onClose} aria-label="Close">
            ✕
          </button>
          <AbilityCard ability={ability} />
        </div>
      </div>
    </Portal>
  )
}
