export function CoinIcon({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 24 24" fill="currentColor" className={className} aria-hidden="true" focusable="false">
      <circle cx="12" cy="12" r="9" />
      <circle cx="12" cy="12" r="6.5" fill="none" stroke="rgba(0,0,0,0.25)" strokeWidth="1" />
      <text x="12" y="16" textAnchor="middle" fontSize="10" fontWeight="700" fill="rgba(0,0,0,0.35)">
        $
      </text>
    </svg>
  )
}
