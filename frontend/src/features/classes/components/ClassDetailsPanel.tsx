import type { ClassDefinition } from '../types'
import { StatBar } from './StatBar'
import { AbilityCard } from './AbilityCard'

export function ClassDetailsPanel({ definition }: { definition: ClassDefinition }) {
  const { characterClass, stats } = definition

  return (
    <div className={`class-details-panel class-${characterClass.toLowerCase()}`}>
      <p className="class-flavor">{definition.flavorText}</p>

      <div className="stat-bar-list">
        <StatBar label="Health" value={stats.maxHealth} max={200} characterClass={characterClass} />
        <StatBar label="Damage" value={stats.damage} max={30} characterClass={characterClass} />
        <StatBar label="Defense" value={stats.defense} max={30} characterClass={characterClass} />
        <StatBar label="Speed" value={stats.speed} max={15} characterClass={characterClass} />
        <StatBar label="Stamina" value={stats.maxStamina} max={150} characterClass={characterClass} />
      </div>

      <div className="ability-list">
        {definition.abilities.map((ability) => (
          <AbilityCard key={ability.id} ability={ability} />
        ))}
      </div>
    </div>
  )
}
