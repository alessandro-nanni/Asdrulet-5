export function LoginButton() {
  function handleLogin() {
    window.location.href = '/oauth2/authorization/google'
  }

  return (
    <button type="button" className="btn btn-primary btn-block" onClick={handleLogin}>
      Sign in with Google
    </button>
  )
}
