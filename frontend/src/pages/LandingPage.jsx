import { Link } from 'react-router-dom'
import './LandingPage.css'

function LandingPage() {
  return (
    <div className="landing">
      {/* ── HERO ─────────────────────────────────────────────── */}
      <header className="landing__hero">
        <div className="landing__hero-inner">
          {/* Logo */}
          <div className="landing__logo" aria-label="FletesCo">
            <span className="landing__logo-icon">🚛</span>
            <span className="landing__logo-text">FletesCo</span>
          </div>

          {/* Eslogan */}
          <h1 className="landing__headline">Tu carga en buenas manos</h1>

          {/* Descripción */}
          <p className="landing__description">
            Conectamos conductores y despachadores en Colombia para hacer
            el transporte de carga más rápido, seguro y transparente.
            Publica solicitudes, acepta fletes y sigue cada envío en
            tiempo real, todo desde un mismo lugar.
          </p>

          {/* CTAs */}
          <div className="landing__ctas">
            <Link to="/login" className="landing__btn landing__btn--primary">
              Iniciar sesión
            </Link>
            <Link to="/registro" className="landing__btn landing__btn--outline">
              Registrarme
            </Link>
          </div>
        </div>

        {/* Decoración de fondo */}
        <div className="landing__blob landing__blob--1" aria-hidden="true" />
        <div className="landing__blob landing__blob--2" aria-hidden="true" />
      </header>

      {/* ── FEATURES ─────────────────────────────────────────── */}
      <section className="landing__features" aria-label="Características">
        <FeatureCard
          icon="📦"
          title="Publica tu solicitud"
          desc="Despachadores crean solicitudes de carga con origen, destino, dimensiones y presupuesto en minutos."
        />
        <FeatureCard
          icon="🗺️"
          title="Seguimiento en tiempo real"
          desc="Monitorea la ubicación del conductor durante todo el trayecto desde el panel de control."
        />
        <FeatureCard
          icon="⭐"
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

function FeatureCard({ icon, title, desc }) {
  return (
    <article className="feature-card">
      <span className="feature-card__icon" aria-hidden="true">{icon}</span>
      <h2 className="feature-card__title">{title}</h2>
      <p className="feature-card__desc">{desc}</p>
    </article>
  )
}

export default LandingPage
