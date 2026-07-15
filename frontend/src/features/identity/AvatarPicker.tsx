import {useRef, useState} from 'react'
import {compressImageToDataUrl, ImageProcessingError} from './imageToDataUrl'

interface Props {
    value: string | null
    onChange: (dataUrl: string) => void
}

export function AvatarPicker({value, onChange}: Props) {
    const inputRef = useRef<HTMLInputElement>(null)
    const [error, setError] = useState<string | null>(null)
    const [isProcessing, setIsProcessing] = useState(false)

    async function handleFileChange(event: React.ChangeEvent<HTMLInputElement>) {
        const file = event.target.files?.[0]
        event.target.value = ''
        if (!file) {
            return
        }
        setError(null)
        setIsProcessing(true)
        try {
            onChange(await compressImageToDataUrl(file))
        } catch (caught) {
            setError(caught instanceof ImageProcessingError ? caught.message : 'Could not process that image.')
        } finally {
            setIsProcessing(false)
        }
    }

    return (
        <div className="field avatar-picker">
            <span className="field-label">Profile picture</span>
            <button
                type="button"
                className="avatar-picker-button"
                onClick={() => inputRef.current?.click()}
                disabled={isProcessing}
                aria-label={value ? 'Change profile picture' : 'Choose a profile picture'}
            >
                {value ? (
                    <img src={value} alt="" className="avatar-picker-preview"/>
                ) : (
                    <span className="avatar-picker-placeholder" aria-hidden="true">
            +
          </span>
                )}
            </button>
            <input
                ref={inputRef}
                type="file"
                accept="image/*"
                className="avatar-picker-input"
                onChange={handleFileChange}
            />
            {isProcessing && <p className="muted">Processing image...</p>}
            {error && (
                <p className="alert" role="alert">
                    {error}
                </p>
            )}
        </div>
    )
}
