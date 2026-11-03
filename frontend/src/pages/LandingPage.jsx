import { Link } from 'react-router-dom'
import Logo from '../components/Logo'
import './LandingPage.css'

function LandingPage() {
  return (
    <div className="landing">
      {/* ── NAVEGACIÓN ───────────────────────────────────────── */}
      <nav className="landing__nav" aria-label="Principal">
        <Link to="/" className="landing__nav-logo" aria-label="FletesCo, inicio">
          <Logo />
        </Link>
        <div className="landing__nav-links">
          <a href="#funciones">Funciones</a>
          <Link to="/login">Iniciar sesión</Link>
        </div>
      </nav>

      {/* ── HERO ─────────────────────────────────────────────── */}
      <header className="landing__hero">
        <div className="landing__hero-inner">
          <p className="landing__eyebrow">Transporte de carga en Colombia</p>

          {/* Eslogan */}
          <h1 className="landing__headline">Tu carga en buenas manos</h1>

          {/* Descripción */}
          <p className="landing__description">
            Conectamos conductores y despachadores en Colombia para hacer
            el transporte de carga más rápido, seguro y transparente.
            Publica solicitudes, acepta fletes y sigue cada envío en
            tiempo real, todo desde un mismo lugar.
          </p>

          {/* CTAs: un solo botón naranja por pantalla */}
          <div className="landing__ctas">
            <Link to="/registro" className="landing__btn landing__btn--primary">
              Registrarme
            </Link>
            <Link to="/login" className="landing__btn landing__btn--ghost">
              Iniciar sesión
            </Link>
          </div>
        </div>

        <VentanaDemo />
      </header>

      {/* ── FUNCIONES ────────────────────────────────────────── */}
      <section id="funciones" className="landing__features" aria-label="Características">
        <FeatureCard
          icon={<IconoCaja />}
          title="Publica tu solicitud"
          desc="Despachadores crean solicitudes de carga con origen, destino, dimensiones y presupuesto en minutos."
        />
        <FeatureCard
          icon={<IconoRuta />}
          title="Seguimiento en tiempo real"
          desc="Monitorea la ubicación del conductor durante todo el trayecto desde el panel de control."
        />
        <FeatureCard
          icon={<IconoEstrella />}
          title="Sistema de calificaciones"
          desc="Construye reputación con cada viaje. Conductores y despachadores se evalúan mutuamente."
        />
      </section>

      {/* ── FOOTER ───────────────────────────────────────────── */}
      <footer className="landing__footer">
        <p>© {new Date().getFullYear()} FletesCo — Todos los derechos reservados.</p>
      </footer>
    </div>
  )
}

/* Vista previa decorativa del panel, dentro de un marco de ventana. */
function VentanaDemo() {
  return (
    <div className="ventana" aria-hidden="true">
      <div className="ventana__barra">
        <span className="ventana__punto ventana__punto--rojo" />
        <span className="ventana__punto ventana__punto--ambar" />
        <span className="ventana__punto ventana__punto--verde" />
        <span className="ventana__url">fletesco · solicitudes</span>
      </div>
      <div className="ventana__cuerpo">
        <div className="demo-card">
          <div className="demo-card__fila">
            <span className="demo-card__titulo">Medellín → Bogotá</span>
            <span className="demo-badge demo-badge--azul">En curso</span>
          </div>
          <div className="demo-card__fila">
            <span className="demo-card__dato">Placa <b>TKX-482</b></span>
            <span className="demo-card__precio">$ 1.850.000</span>
          </div>
          <div className="demo-progreso"><span style={{ width: '62%' }} /></div>
        </div>
        <div className="demo-card">
          <div className="demo-card__fila">
            <span className="demo-card__titulo">Cali → Pereira</span>
            <span className="demo-badge demo-badge--ambar">Publicada</span>
          </div>
          <div className="demo-card__fila">
            <span className="demo-card__dato">Carga <b>2,4 t</b></span>
            <span className="demo-card__precio">$ 920.000</span>
          </div>
        </div>
        <div className="demo-card">
          <div className="demo-card__fila">
            <span className="demo-card__titulo">Barranquilla → Cartagena</span>
            <span className="demo-badge demo-badge--verde">Completada</span>
          </div>
          <div className="demo-card__fila">
            <span className="demo-card__dato">Cita <b>#A-1093</b></span>
            <span className="demo-card__precio">$ 640.000</span>
          </div>
        </div>
      </div>
    </div>
  )
}

function FeatureCard({ icon, title, desc }) {
  return (
    <article className="feature-card">
      <span className="feature-card__icon" aria-hidden="true">{icon}</span>
      <h2 className="feature-card__title">{title}</h2>
      <p className="feature-card__desc">{desc}</p>
    </article>
  )
}

const svgProps = {
  width: 22,
  height: 22,
  viewBox: '0 0 24 24',
  fill: 'none',
  stroke: 'currentColor',
  strokeWidth: 1.6,
  strokeLinecap: 'round',
  strokeLinejoin: 'round',
}

function IconoCaja() {
  return (
    <svg {...svgProps}>
      <path d="M21 8 12 3 3 8v8l9 5 9-5V8Z" />
      <path d="m3 8 9 5 9-5M12 13v8" />
    </svg>
  )
}

function IconoRuta() {
  return (
    <svg {...svgProps}>
      <circle cx="6" cy="19" r="2" />
      <circle cx="18" cy="5" r="2" />
      <path d="M8 19h8.5a3.5 3.5 0 0 0 0-7h-9a3.5 3.5 0 0 1 0-7H16" />
    </svg>
  )
}

function IconoEstrella() {
  return (
    <svg {...svgProps}>
      <path d="m12 3 2.8 5.7 6.2.9-4.5 4.4 1 6.2L12 17.3 6.5 20.2l1-6.2L3 9.6l6.2-.9L12 3Z" />
    </svg>
  )
}

export default LandingPage
