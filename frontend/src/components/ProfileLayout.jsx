import { Link } from 'react-router-dom'
import { useSesion } from '../context/useSesion'
import Logo from './Logo'

function ProfileLayout({ role, title, subtitle, children }) {
  const { cerrarSesion } = useSesion()

  // POST /api/auth/logout borra la cookie; luego se limpia la sesión y se va a /login.
  const handleLogout = () => {
    cerrarSesion()
  }

  return (
    <main className="app-shell">
      <aside className="side-panel">
        <Logo />
        <nav className="profile-nav" aria-label="Perfiles">
          {role === 'Perfil de conductor' && <Link to="/perfil/conductor">Conductor</Link>}
          {role === 'Perfil de despachador' && <Link to="/perfil/despachador">Despachador</Link>}
          {role === 'Panel de Administrador' && <Link to="/perfil/admin">Administrador</Link>}
        </nav>
      </aside>

      <section className="workspace">
        <header className="workspace-header">
          <div>
            <p className="eyebrow">{role}</p>
            <h1>{title}</h1>
            <p className="subtitle">{subtitle}</p>
          </div>
          <button className="secondary-button" type="button" onClick={handleLogout}>
            Salir
          </button>
        </header>
        {children}
      </section>
    </main>
  )
}

export default ProfileLayout
