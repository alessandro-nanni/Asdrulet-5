// The game's sprites are authored at various native pixel resolutions, capped
// at 64x64. Rather than stretching every sprite to a uniform display size,
// display size scales proportionally to native resolution — a 16x16 sprite
// renders 4x smaller than a 64x64 one — so lower-res placeholder art doesn't
// get blown up to match higher-res pieces.
const MAX_NATIVE_SIZE = 64
const DISPLAY_SIZE_AT_MAX = 28

interface Props {
    src: string
    nativeSize: number
    alt: string
    className?: string
}

export function PixelSprite({src, nativeSize, alt, className}: Props) {
    const displaySize = (nativeSize / MAX_NATIVE_SIZE) * DISPLAY_SIZE_AT_MAX

    return (
        <img
            src={src}
            alt={alt}
            width={displaySize}
            height={displaySize}
            className={className}
            style={{imageRendering: 'pixelated'}}
        />
    )
}
