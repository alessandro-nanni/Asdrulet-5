export function LoginButton() {
  function handleLogin() {
    window.location.href = '/oauth2/authorization/google'
  }

  return (
    <button type="button" onClick={handleLogin}>
      Sign in with Google
    </button>
  )
}
