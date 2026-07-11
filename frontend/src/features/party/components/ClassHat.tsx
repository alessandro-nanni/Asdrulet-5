import { PixelSprite } from '../../../shared/ui/PixelSprite'
import type { CharacterClass } from '../types'
import healerHat from '../../../assets/hats/healer.png'
import tankHat from '../../../assets/hats/tank.png'
import warriorHat from '../../../assets/hats/warrior.png'
import mageHat from '../../../assets/hats/mage.png'

const HAT_SOURCES: Record<CharacterClass, string> = {
  HEALER: healerHat,
  TANK: tankHat,
  WARRIOR: warriorHat,
  MAGE: mageHat,
}

export function ClassHat({ characterClass }: { characterClass: CharacterClass }) {
  return (
    <PixelSprite
      src={HAT_SOURCES[characterClass]}
      nativeSize={64}
      alt={`${characterClass} hat`}
      className="class-hat"
    />
  )
}
