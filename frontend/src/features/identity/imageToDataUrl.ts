const MAX_DIMENSION = 128
const JPEG_QUALITY = 0.75
const MAX_INPUT_BYTES = 10 * 1024 * 1024

export class ImageProcessingError extends Error {
}

/**
 * Turns a user-picked image file into a small, self-contained JPEG data URL
 * suitable for storing in localStorage and broadcasting to the rest of the
 * party. Deliberately decodes and re-draws the image onto a canvas rather
 * than reading the file's raw bytes (FileReader.readAsDataURL) — that would
 * pass through whatever the file actually contains under an image
 * extension, which createImageBitmap won't accept unless it's real,
 * decodable image data. Downscaling to a small fixed size keeps the
 * resulting string well under the backend's size cap.
 */
export async function compressImageToDataUrl(file: File): Promise<string> {
    if (!file.type.startsWith('image/')) {
        throw new ImageProcessingError('Please choose an image file.')
    }
    if (file.size > MAX_INPUT_BYTES) {
        throw new ImageProcessingError('That image is too large — please pick one under 10 MB.')
    }

    let bitmap: ImageBitmap
    try {
        bitmap = await createImageBitmap(file)
    } catch {
        throw new ImageProcessingError('That file could not be read as an image.')
    }

    const scale = Math.min(1, MAX_DIMENSION / Math.max(bitmap.width, bitmap.height))
    const width = Math.max(1, Math.round(bitmap.width * scale))
    const height = Math.max(1, Math.round(bitmap.height * scale))

    const canvas = document.createElement('canvas')
    canvas.width = width
    canvas.height = height
    const ctx = canvas.getContext('2d')
    if (!ctx) {
        bitmap.close()
        throw new ImageProcessingError('Your browser cannot process images.')
    }
    ctx.drawImage(bitmap, 0, 0, width, height)
    bitmap.close()

    return canvas.toDataURL('image/jpeg', JPEG_QUALITY)
}
