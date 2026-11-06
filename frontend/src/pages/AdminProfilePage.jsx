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
            <span className="status-chip status-chip--activo">
              ● SESIÓN ACTIVA
            </span>
          </div>

          <p className="admin-intro">
            ¡La redirección y autenticación para el usuario <strong>ADMINISTRADOR</strong> funcionan correctamente!
            Desde este panel podrás supervisar las operaciones de conductores, despachadores y solicitudes en la plataforma.
          </p>

          <div className="admin-metricas">
            <div className="admin-metrica">
              <span className="admin-metrica__etiqueta">ROL DE USUARIO</span>
              <p className="admin-metrica__valor admin-metrica__valor--azul">
                {adminInfo.tipoUsuario}
              </p>
            </div>

            <div className="admin-metrica">
              <span className="admin-metrica__etiqueta">ESTADO EN BD</span>
              <p className="admin-metrica__valor admin-metrica__valor--verde">
                ACTIVO
              </p>
            </div>

            <div className="admin-metrica">
              <span className="admin-metrica__etiqueta">PROTECCIÓN DE RUTA</span>
              <p className="admin-metrica__valor admin-metrica__valor--azul">
                HABILITADA
              </p>
            </div>
          </div>

          <div className="admin-modulos">
            <h3>Módulos del Sistema</h3>
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

          <div className="vehicle-card admin-cuenta">
            <span>Tipo de Cuenta</span>
            <strong>{adminInfo.tipoUsuario}</strong>
            <small>AUTORIZADO</small>
          </div>
        </aside>
      </div>
    </ProfileLayout>
  )
}

export default AdminProfilePage
