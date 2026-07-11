import { useState } from 'react'
import type { CharacterClass, PartyMember } from '../types'
import type { ClassDefinition } from '../../classes/types'
import { ClassDetailsPanel } from '../../classes/components/ClassDetailsPanel'

const CLASSES: CharacterClass[] = ['HEALER', 'TANK', 'WARRIOR', 'MAGE']

interface Props {
  members: PartyMember[]
  selfUserId: string
  onSelect: (characterClass: CharacterClass) => void
  definitions: ClassDefinition[]
}

export function ClassSelector({ members, selfUserId, onSelect, definitions }: Props) {
  const self = members.find((member) => member.userId === selfUserId)
  const [expandedClass, setExpandedClass] = useState<CharacterClass | null>(self?.characterClass ?? null)

  const takenBy = new Map<CharacterClass, string>()
  for (const member of members) {
    if (member.characterClass && member.userId !== selfUserId) {
      takenBy.set(member.characterClass, member.displayName)
    }
  }

  function handleCardClick(characterClass: CharacterClass) {
    onSelect(characterClass)
    setExpandedClass(characterClass)
  }

  const expandedDefinition = definitions.find((definition) => definition.characterClass === expandedClass) ?? null

  return (
    <div className="class-selector">
      <div className="class-grid">
        {CLASSES.map((characterClass) => {
          const takenByName = takenBy.get(characterClass)
          const isSelected = self?.characterClass === characterClass

          return (
            <button
              key={characterClass}
              type="button"
              className={`class-card class-${characterClass.toLowerCase()} ${isSelected ? 'is-selected' : ''}`}
              onClick={() => handleCardClick(characterClass)}
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
      {expandedDefinition && <ClassDetailsPanel definition={expandedDefinition} />}
    </div>
  )
}
