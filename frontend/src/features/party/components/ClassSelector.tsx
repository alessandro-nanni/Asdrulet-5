import type { CharacterClass, PartyMember } from '../types'

const CLASSES: CharacterClass[] = ['HEALER', 'TANK', 'WARRIOR', 'MAGE']

interface Props {
  members: PartyMember[]
  selfUserId: string
  onSelect: (characterClass: CharacterClass) => void
}

export function ClassSelector({ members, selfUserId, onSelect }: Props) {
  const self = members.find((member) => member.userId === selfUserId)

  const takenBy = new Map<CharacterClass, string>()
  for (const member of members) {
    if (member.characterClass && member.userId !== selfUserId) {
      takenBy.set(member.characterClass, member.displayName)
    }
  }

  return (
    <div className="class-grid">
      {CLASSES.map((characterClass) => {
        const takenByName = takenBy.get(characterClass)
        const isSelected = self?.characterClass === characterClass

        return (
          <button
            key={characterClass}
            type="button"
            className={`class-card class-${characterClass.toLowerCase()} ${isSelected ? 'is-selected' : ''}`}
            onClick={() => onSelect(characterClass)}
            disabled={Boolean(takenByName)}
          >
            <span className="class-name">{characterClass}</span>
            {takenByName ? (
              <span className="class-status">Taken by {takenByName}</span>
            ) : (
              <span className="class-status">{isSelected ? 'Selected' : 'Available'}</span>
            )}
          </button>
        )
      })}
    </div>
  )
}
