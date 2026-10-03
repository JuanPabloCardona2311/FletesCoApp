import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import ProfileLayout from '../components/ProfileLayout'

function AdminProfilePage() {
  const navigate = useNavigate()
  const [adminInfo, setAdminInfo] = useState({
    nombre: '',
    email: '',
    tipoUsuario: '',
  })

  useEffect(() => {
    const token = localStorage.getItem('fleteco_token')
    const tipoUsuario = localStorage.getItem('fleteco_tipo_usuario')
    const email = localStorage.getItem('fleteco_email') || 'admin@fleteco.com'
    const nombre = localStorage.getItem('fleteco_nombre') || 'Administrador FletesCo'

    if (!token || tipoUsuario !== 'ADMINISTRADOR') {
      navigate('/')
      return
    }

    setAdminInfo({
      nombre,
      email,
      tipoUsuario,
    })
  }, [navigate])

  return (
    <ProfileLayout
      role="Panel de Administrador"
      title="Panel de Control General"
      subtitle="Sesión autenticada con privilegios de Administrador del sistema."
    >
      <div className="profile-grid">
        <section className="profile-panel">
          <div className="panel-heading">
            <h2>Acceso de Administrador Confirmado</h2>
            <span className="status-chip" style={{ background: 'color-mix(in srgb, var(--success) 20%, white)', color: 'var(--success)' }}>
              ● SESIÓN ACTIVA
            </span>
          </div>

          <p style={{ marginTop: 0, color: 'var(--muted)', lineHeight: '1.6' }}>
            ¡La redirección y autenticación para el usuario <strong>ADMINISTRADOR</strong> funcionan correctamente!
            Desde este panel podrás supervisar las operaciones de conductores, despachadores y solicitudes en la plataforma.
          </p>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '16px', marginTop: '24px' }}>
            <div style={{ padding: '16px', border: '1px solid var(--border)', borderRadius: '8px', background: 'var(--paper)' }}>
              <span style={{ fontSize: '0.8rem', color: 'var(--muted)', fontWeight: 600 }}>ROL DE USUARIO</span>
              <p style={{ margin: '8px 0 0', fontSize: '1.2rem', fontWeight: 700, color: 'var(--primary)' }}>
                {adminInfo.tipoUsuario}
              </p>
            </div>

            <div style={{ padding: '16px', border: '1px solid var(--border)', borderRadius: '8px', background: 'var(--paper)' }}>
              <span style={{ fontSize: '0.8rem', color: 'var(--muted)', fontWeight: 600 }}>ESTADO EN BD</span>
              <p style={{ margin: '8px 0 0', fontSize: '1.2rem', fontWeight: 700, color: 'var(--success)' }}>
                ACTIVO
              </p>
            </div>

            <div style={{ padding: '16px', border: '1px solid var(--border)', borderRadius: '8px', background: 'var(--paper)' }}>
              <span style={{ fontSize: '0.8rem', color: 'var(--muted)', fontWeight: 600 }}>PROTECCIÓN DE RUTA</span>
              <p style={{ margin: '8px 0 0', fontSize: '1.2rem', fontWeight: 700, color: 'var(--primary)' }}>
                HABILITADA
              </p>
            </div>
          </div>

          <div style={{ marginTop: '28px', display: 'grid', gap: '12px' }}>
            <h3 style={{ margin: 0, fontSize: '1.1rem' }}>Módulos del Sistema</h3>
            <Link className="vehicle-card" to="/admin/usuarios">Gestionar usuarios</Link>
            <Link className="vehicle-card" to="/admin/disputas">Gestionar disputas</Link>
            <Link className="vehicle-card" to="/admin/reportes">Ver reportes</Link>
          </div>
        </section>

        <aside className="summary-panel">
          <h2>{adminInfo.nombre}</h2>
          <p>{adminInfo.email}</p>

          <div className="metric-list">
            <div>
              <span>Panel</span>
              <strong>Administración</strong>
            </div>
            <div>
              <span>Accesos</span>
              <strong>3 módulos</strong>
            </div>
          </div>

          <div className="vehicle-card" style={{ borderColor: 'var(--primary)' }}>
            <span>Tipo de Cuenta</span>
            <strong>{adminInfo.tipoUsuario}</strong>
            <small style={{ color: 'var(--success)' }}>AUTORIZADO</small>
          </div>
        </aside>
      </div>
    </ProfileLayout>
  )
}

export default AdminProfilePage
