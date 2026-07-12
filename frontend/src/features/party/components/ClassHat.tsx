import type { CharacterClass } from '../types'
import healerHat from '../../../assets/hats/healer.svg'
import tankHat from '../../../assets/hats/tank.svg'
import warriorHat from '../../../assets/hats/warrior.svg'
import mageHat from '../../../assets/hats/mage.svg'

const HAT_SOURCES: Record<CharacterClass, string> = {
  HEALER: healerHat,
  TANK: tankHat,
  WARRIOR: warriorHat,
  MAGE: mageHat,
}

export function ClassHat({ characterClass }: { characterClass: CharacterClass }) {
  return (
    <img
      src={HAT_SOURCES[characterClass]}
      alt={`${characterClass} hat`}
      className="class-hat"
    />
  )
}
