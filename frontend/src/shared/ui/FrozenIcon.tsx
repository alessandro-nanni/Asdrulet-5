export function FrozenIcon({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 24 24" fill="currentColor" className={className} aria-hidden="true" focusable="false">
      <rect x="11" y="1" width="2" height="22" rx="1" />
      <rect x="11" y="1" width="2" height="22" rx="1" transform="rotate(60 12 12)" />
      <rect x="11" y="1" width="2" height="22" rx="1" transform="rotate(120 12 12)" />
    </svg>
  )
}
