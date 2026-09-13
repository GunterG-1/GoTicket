export default function LoginPage() {
  function startGoogleLogin() {
    const apiManagerUrl = import.meta.env.VITE_API_MANAGER_URL || 'http://localhost:8080';
    window.location.href = `${apiManagerUrl}/oauth2/authorization/google`;
  }

  return <main className="auth-shell"><div className="auth-panel">
    <p className="eyebrow">GOTICKET / ACCESO</p>
    <h1>Tu agenda de experiencias.</h1>
    <p className="muted">Inicia sesión para consultar eventos, reservar entradas y revisar tus pagos.</p>
    <button className="primary-button oauth-button" onClick={startGoogleLogin}>Continuar con Google</button>
  </div></main>;
}
