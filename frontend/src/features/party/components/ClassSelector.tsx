import type { CharacterClass } from '../types'

const CLASSES: CharacterClass[] = ['HEALER', 'TANK', 'WARRIOR', 'MAGE']

interface Props {
  selected: CharacterClass | null
  onSelect: (characterClass: CharacterClass) => void
}

export function ClassSelector({ selected, onSelect }: Props) {
  return (
    <div>
      {CLASSES.map((characterClass) => (
        <button
          key={characterClass}
          type="button"
          onClick={() => onSelect(characterClass)}
          disabled={characterClass === selected}
        >
          {characterClass}
        </button>
      ))}
    </div>
  )
}
