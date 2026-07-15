interface Props {
    label: string
    value: number
    max: number
    /** Lowercase theme key, e.g. 'berserker' or 'enemy' — see the `.stat-bar.class-*` rules in index.css. */
    theme: string
}

export function StatBar({label, value, max, theme}: Props) {
    const fillPercent = Math.min(100, Math.round((value / max) * 100))

    return (
        <div className={`stat-bar class-${theme}`}>
            <div className="stat-bar-label">
                <span>{label}</span>
                <span>{value}</span>
            </div>
            <div className="stat-bar-track">
                <div className="stat-bar-fill" style={{width: `${fillPercent}%`}}/>
            </div>
        </div>
    )
}
