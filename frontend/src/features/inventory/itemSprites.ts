import scythe from '../../assets/items/scythe.png'
import torch from '../../assets/items/torch.png'
import luckyCharm from '../../assets/items/lucky-charm.png'
import satelliteDish from '../../assets/items/satellite-dish.png'
import twitchingTalisman from '../../assets/items/twitching-talisman.png'
import leatherTunic from '../../assets/items/leather-tunic.png'
import mantleOfTheUsurper from '../../assets/items/mantle-of-the-usurper.png'

// Keyed by ItemDefinition.id (see backend ItemDefinitionRegistry) rather than
// slot, since each slot has several distinct items.
export const ITEM_SPRITES: Record<string, string> = {
  'scythe': scythe,
  'torch': torch,
  'lucky-charm': luckyCharm,
  'satellite-dish': satelliteDish,
  'twitching-talisman': twitchingTalisman,
  'leather-tunic': leatherTunic,
  'mantle-of-the-usurper': mantleOfTheUsurper,
}
