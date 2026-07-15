import type { CharacterClass } from '../types'
import healerHat from '../../../assets/hats/healer.png'
import paladinHat from '../../../assets/hats/paladin.png'
import berserkerHat from '../../../assets/hats/berserker.png'
import mageHat from '../../../assets/hats/mage.png'

const HAT_SOURCES: Record<CharacterClass, string> = {
  HEALER: healerHat,
  PALADIN: paladinHat,
  BERSERKER: berserkerHat,
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
