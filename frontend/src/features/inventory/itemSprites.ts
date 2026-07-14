import rustedSword from '../../assets/items/rusted-sword.png'
import flameEdge from '../../assets/items/flame-edge.png'
import vampiricFang from '../../assets/items/vampiric-fang.png'
import leatherVest from '../../assets/items/leather-vest.png'
import plateArmor from '../../assets/items/plate-armor.png'
import thornedPlate from '../../assets/items/thorned-plate.png'
import luckyCharm from '../../assets/items/lucky-charm.png'
import berserkersRing from '../../assets/items/berserkers-ring.png'
import executionersBadge from '../../assets/items/executioners-badge.png'

// Keyed by ItemDefinition.id (see backend ItemDefinitionRegistry) rather than
// slot, since each slot has several distinct items.
export const ITEM_SPRITES: Record<string, string> = {
  'rusted-sword': rustedSword,
  'flame-edge': flameEdge,
  'vampiric-fang': vampiricFang,
  'leather-vest': leatherVest,
  'plate-armor': plateArmor,
  'thorned-plate': thornedPlate,
  'lucky-charm': luckyCharm,
  'berserkers-ring': berserkersRing,
  'executioners-badge': executionersBadge,
}
