interface Props {
  value: string
  onChange: (value: string) => void
}

export function UsernameField({ value, onChange }: Props) {
  return (
    <div className="field">
      <label className="field-label" htmlFor="username">
        Your name
      </label>
      <input
        id="username"
        className="input"
        value={value}
        onChange={(event) => onChange(event.target.value)}
        placeholder="e.g. Grog"
        maxLength={24}
        autoComplete="off"
      />
    </div>
  )
}
