import {useState} from 'react'
import type {CharacterClass, PartyMember} from '../types'
import type {ClassDefinition} from '../../classes/types'
import {ClassInfoOverlay} from './ClassInfoOverlay'

const CLASSES: CharacterClass[] = ['HEALER', 'PALADIN', 'BERSERKER', 'MAGE']

interface Props {
    members: PartyMember[]
    selfUserId: string
    onSelect: (characterClass: CharacterClass) => void
    definitions: ClassDefinition[]
}

export function ClassSelector({members, selfUserId, onSelect, definitions}: Props) {
    const self = members.find((member) => member.userId === selfUserId)
    const [infoClass, setInfoClass] = useState<CharacterClass | null>(null)

    const takenBy = new Map<CharacterClass, string>()
    for (const member of members) {
        if (member.characterClass && member.userId !== selfUserId) {
            takenBy.set(member.characterClass, member.displayName)
        }
    }

    const infoDefinition = definitions.find((definition) => definition.characterClass === infoClass) ?? null

    return (
        <div className="class-selector">
            <div className="class-grid">
                {CLASSES.map((characterClass) => {
                    const takenByName = takenBy.get(characterClass)
                    const isSelected = self?.characterClass === characterClass

                    return (
                        <div key={characterClass} className="class-card-wrap">
                            <button
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
                            <button
                                type="button"
                                className="class-info-btn"
                                aria-label={`View ${characterClass} details`}
                                onClick={(event) => {
                                    event.stopPropagation()
                                    setInfoClass(characterClass)
                                }}
                            >
                                ?
                            </button>
                        </div>
                    )
                })}
            </div>
            {infoDefinition && <ClassInfoOverlay definition={infoDefinition} onClose={() => setInfoClass(null)}/>}
        </div>
    )
}
