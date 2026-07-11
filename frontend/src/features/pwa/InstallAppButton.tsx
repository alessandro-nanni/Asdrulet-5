import { useInstallPrompt } from './useInstallPrompt'

export function InstallAppButton() {
  const { canInstall, promptInstall } = useInstallPrompt()

  if (!canInstall) {
    return null
  }

  return (
    <button type="button" className="btn btn-secondary btn-block" onClick={promptInstall}>
      Install app
    </button>
  )
}
