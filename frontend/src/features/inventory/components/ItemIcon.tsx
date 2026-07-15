import {ITEM_SPRITES} from '../itemSprites'

interface Props {
    itemId: string
    alt?: string
    className?: string
}

export function ItemIcon({itemId, alt = '', className}: Props) {
    const src = ITEM_SPRITES[itemId]
    if (!src) {
        return null
    }
    return <img src={src} alt={alt} className={className}/>
}
