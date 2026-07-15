import { useEffect, useRef, useState } from 'react'
import { CoinIcon } from '../../../shared/ui/CoinIcon'

interface Props {
  coins: number
}

interface Popup {
  key: number
  delta: number
}

const TOAST_DURATION_MS = 1800

export function CoinChangeToast({ coins }: Props) {
  const previousRef = useRef<number | null>(null)
  const keyRef = useRef(0)
  const [popup, setPopup] = useState<Popup | null>(null)

  useEffect(() => {
    const previous = previousRef.current
    previousRef.current = coins
    if (previous === null || previous === coins) {
      return
    }
    const entry: Popup = { key: keyRef.current++, delta: coins - previous }
    setPopup(entry)
    const timer = setTimeout(() => {
      setPopup((current) => (current?.key === entry.key ? null : current))
    }, TOAST_DURATION_MS)
    return () => clearTimeout(timer)
  }, [coins])

  if (!popup) {
    return null
  }

  return (
    <div key={popup.key} className={`coin-toast ${popup.delta > 0 ? 'is-gain' : 'is-loss'}`}>
      <CoinIcon className="coin-toast-icon" />
      <span>
        {popup.delta > 0 ? '+' : ''}
        {popup.delta}
      </span>
    </div>
  )
}
