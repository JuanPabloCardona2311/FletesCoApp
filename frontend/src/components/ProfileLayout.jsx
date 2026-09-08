import { Link, useNavigate } from 'react-router-dom'
import heroImage from '../assets/hero.png'
import Logo from './Logo'

function ProfileLayout({ role, title, subtitle, children }) {
  const navigate = useNavigate()

  const handleLogout = () => {
    localStorage.removeItem('fleteco_token')
    localStorage.removeItem('fleteco_tipo_usuario')
    navigate('/')
  }

  return (
    <main className="app-shell">
      <aside className="side-panel">
        <Logo />
        <img className="side-panel__image" src={heroImage} alt="" />
        <nav className="profile-nav" aria-label="Perfiles">
          <Link to="/perfil/conductor">Conductor</Link>
          <Link to="/perfil/despachador">Despachador</Link>
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
