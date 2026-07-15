export function TauntIcon({className}: { className?: string }) {
    return (
        <svg viewBox="0 0 24 24" fill="currentColor" className={className} aria-hidden="true" focusable="false">
            <circle cx="12" cy="12" r="3.5"/>
            <rect x="11" y="1" width="2" height="5" rx="1"/>
            <rect x="11" y="18" width="2" height="5" rx="1"/>
            <rect x="1" y="11" width="5" height="2" rx="1"/>
            <rect x="18" y="11" width="5" height="2" rx="1"/>
        </svg>
    )
}
