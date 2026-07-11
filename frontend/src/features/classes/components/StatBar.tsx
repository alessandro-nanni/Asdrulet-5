import type { CharacterClass } from '../../party/types'

interface Props {
  label: string
  value: number
  max: number
  characterClass: CharacterClass
}

export function StatBar({ label, value, max, characterClass }: Props) {
  const fillPercent = Math.min(100, Math.round((value / max) * 100))

  return (
    <div className={`stat-bar class-${characterClass.toLowerCase()}`}>
      <div className="stat-bar-label">
        <span>{label}</span>
        <span>{value}</span>
      </div>
      <div className="stat-bar-track">
        <div className="stat-bar-fill" style={{ width: `${fillPercent}%` }} />
      </div>
    </div>
  )
}
