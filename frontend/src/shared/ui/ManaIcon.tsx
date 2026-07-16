export function ManaIcon({className}: { className?: string }) {
    return (
        <svg viewBox="0 0 24 24" fill="currentColor" className={className} aria-hidden="true" focusable="false">
            {/* Faceted mana crystal, not a water droplet. */}
            <path d="M12 2L19 9L12 22L5 9Z"/>
            <path
                d="M5 9L19 9M12 2L9 9M12 2L15 9"
                fill="none" stroke="rgba(255,255,255,0.4)" strokeWidth="1" strokeLinejoin="round"/>
            {/* Small twinkle for the "magic" read. */}
            <path d="M18.5 1L19.3 3L21.3 3.8L19.3 4.6L18.5 6.6L17.7 4.6L15.7 3.8L17.7 3Z"/>
        </svg>
    )
}
