import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import client from '../api/client'
import ProfileLayout from '../components/ProfileLayout'
import RequestMessage from '../components/RequestMessage'

const initialForm = {
  nombreEmpresa: '',
  nit: '',
}

const emptyProfile = {
  nombre: '',
  email: '',
  telefono: '',
  nombreEmpresa: '',
  nit: '',
  totalSolicitudesPublicadas: 0,
}

function DespachadorProfilePage() {
  const navigate = useNavigate()
  const [perfil, setPerfil] = useState(emptyProfile)
  const [form, setForm] = useState(initialForm)
  const [mensaje, setMensaje] = useState('')
  const [error, setError] = useState('')
  const [cargando, setCargando] = useState(false)

  const aplicarPerfil = (perfilActual) => {
    setPerfil(perfilActual)
    setForm({
      nombreEmpresa: perfilActual.nombreEmpresa || '',
      nit: perfilActual.nit || '',
    })
  }

  useEffect(() => {
    const cargarPerfil = async () => {
      try {
        const response = await client.get('/api/perfiles/despachador')
        aplicarPerfil(response.data)
      } catch (requestError) {
        if (requestError.response?.status === 401) {
          localStorage.removeItem('fleteco_token')
          localStorage.removeItem('fleteco_tipo_usuario')
          navigate('/')
        } else {
          setError('No fue posible cargar el perfil de despachador.')
        }
      }
    }

    cargarPerfil()
  }, [navigate])

  const handleChange = (event) => {
    setForm({ ...form, [event.target.name]: event.target.value })
  }

  const handleSubmit = async (event) => {
    event.preventDefault()
    setMensaje('')
    setError('')
    setCargando(true)

    try {
      const response = await client.put('/api/perfiles/despachador', {
        nombreEmpresa: form.nombreEmpresa.trim() || null,
        nit: form.nit.trim() || null,
      })
      aplicarPerfil(response.data)
      setMensaje('Perfil de despachador guardado correctamente.')
    } catch (requestError) {
      if (requestError.response?.status === 401) {
        localStorage.removeItem('fleteco_token')
        localStorage.removeItem('fleteco_tipo_usuario')
        navigate('/')
        return
      }

      setError(requestError.response?.data?.error || 'No fue posible guardar el perfil de despachador.')
    } finally {
      setCargando(false)
    }
  }

  return (
    <ProfileLayout role="Perfil de despachador" title="Empresa y operación" subtitle="Datos mínimos para identificar al despachador en FleteCo.">
      <div className="profile-grid">
        <section className="profile-panel">
          <div className="panel-heading">
            <h2>Datos comerciales</h2>
          </div>

          <form onSubmit={handleSubmit}>
            <label>
              Nombre de empresa
              <input name="nombreEmpresa" value={form.nombreEmpresa} onChange={handleChange} placeholder="Transportes del Valle" maxLength="150" />
            </label>
            <label>
              NIT
              <input name="nit" value={form.nit} onChange={handleChange} placeholder="900123456-7" maxLength="20" />
            </label>
            <button type="submit" disabled={cargando}>{cargando ? 'Guardando...' : 'Guardar perfil'}</button>
            <RequestMessage mensaje={mensaje} error={error} />
          </form>
        </section>

        <aside className="summary-panel">
          <h2>{perfil.nombre}</h2>
          <p>{perfil.email}</p>

          <div className="metric-list">
            <div>
              <span>Solicitudes</span>
              <strong>{perfil.totalSolicitudesPublicadas || 0}</strong>
            </div>
            <div>
              <span>Teléfono</span>
              <strong>{perfil.telefono || 'Sin dato'}</strong>
            </div>
          </div>

          <div className="vehicle-card">
            <span>Empresa</span>
            <strong>{perfil.nombreEmpresa || 'Persona natural'}</strong>
            <small>{perfil.nit || 'NIT no registrado'}</small>
          </div>
        </aside>
      </div>
    </ProfileLayout>
  )
}

export default DespachadorProfilePage
