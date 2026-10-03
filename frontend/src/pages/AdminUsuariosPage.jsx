import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import ProfileLayout from '../components/ProfileLayout'
import RequestMessage from '../components/RequestMessage'
import adminClient from '../api/adminClient'

function AdminUsuariosPage() {
  const navigate = useNavigate()
  const [usuarios, setUsuarios] = useState([])
  const [tipo, setTipo] = useState('')
  const [estado, setEstado] = useState('')
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState('')
  const [mensaje, setMensaje] = useState('')

  const handleUnauthorized = (requestError) => {
    if (requestError.response?.status === 401) {
      localStorage.removeItem('fleteco_token')
      localStorage.removeItem('fleteco_tipo_usuario')
      navigate('/')
      return true
    }
    return false
  }

  const cargarUsuarios = async () => {
    setCargando(true)
    setError('')
    try {
      const response = await adminClient.get('/api/admin/usuarios', { params: { tipo, estado } })
      setUsuarios(response.data)
    } catch (requestError) {
      if (!handleUnauthorized(requestError)) {
        setError(requestError.response?.data?.error || 'No fue posible cargar los usuarios.')
      }
    } finally {
      setCargando(false)
    }
  }

  useEffect(() => {
    cargarUsuarios()
  }, [tipo, estado])

  const cambiarEstado = async (usuario) => {
    const nuevoEstado = usuario.estado === 'ACTIVO' ? 'INACTIVO' : 'ACTIVO'
    setMensaje('')
    setError('')
    try {
      await adminClient.patch(`/api/admin/usuarios/${usuario.id}/estado`, { estado: nuevoEstado })
      setMensaje('Estado del usuario actualizado correctamente.')
      await cargarUsuarios()
    } catch (requestError) {
      if (!handleUnauthorized(requestError)) {
        setError(requestError.response?.data?.error || 'No fue posible actualizar el usuario.')
      }
    }
  }

  const emailAdministrador = localStorage.getItem('fleteco_email')

  return (
    <ProfileLayout role="Panel de Administrador" title="Gestión de usuarios" subtitle="Administra el acceso de las cuentas registradas.">
      <p><Link to="/perfil/admin">Volver al panel</Link></p>
      <RequestMessage mensaje={mensaje} error={error} />
      <section className="profile-panel">
        <div className="two-columns">
          <label>Tipo
            <select value={tipo} onChange={(event) => setTipo(event.target.value)}>
              <option value="">Todos</option>
              <option value="ADMINISTRADOR">Administrador</option>
              <option value="CONDUCTOR">Conductor</option>
              <option value="DESPACHADOR">Despachador</option>
            </select>
          </label>
          <label>Estado
            <select value={estado} onChange={(event) => setEstado(event.target.value)}>
              <option value="">Todos</option>
              <option value="ACTIVO">Activo</option>
              <option value="INACTIVO">Inactivo</option>
            </select>
          </label>
        </div>
        {cargando ? <p>Cargando usuarios...</p> : (
          <div style={{ overflowX: 'auto', marginTop: '24px' }}>
            <table>
              <thead><tr><th>Nombre</th><th>Email</th><th>Tipo</th><th>Estado</th><th>Acción</th></tr></thead>
              <tbody>
                {usuarios.map((usuario) => (
                  <tr key={usuario.id}>
                    <td>{usuario.nombre}</td>
                    <td>{usuario.email}</td>
                    <td>{usuario.tipoUsuario}</td>
                    <td>{usuario.estado}</td>
                    <td>
                      <button type="button" disabled={usuario.email === emailAdministrador} onClick={() => cambiarEstado(usuario)}>
                        {usuario.estado === 'ACTIVO' ? 'Inactivar' : 'Activar'}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </ProfileLayout>
  )
}

export default AdminUsuariosPage
