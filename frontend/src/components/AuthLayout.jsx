import { Link } from 'react-router-dom'
import Logo from './Logo'

function AuthLayout({ title, subtitle, children }) {
  return (
    <main className="page">
      <section className="register-card">
        <Link className="auth-logo-link" to="/" aria-label="Volver a la página principal">
          <Logo />
        </Link>
        <h1>{title}</h1>
        <p className="subtitle">{subtitle}</p>
        {children}
      </section>
    </main>
  )
}

export default AuthLayout
