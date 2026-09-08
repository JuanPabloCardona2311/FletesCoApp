import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import client from '../api/client'
import ProfileLayout from '../components/ProfileLayout'
import RequestMessage from '../components/RequestMessage'

const initialForm = {
  nombreEmpresa: '',
  nit: '',
}

const demoProfile = {
  nombre: 'Despachador demo',
  email: 'despachador@fleteco.test',
  telefono: '3100000000',
  nombreEmpresa: '',
  nit: '',
  totalSolicitudesPublicadas: 0,
}

function DespachadorProfilePage() {
  const navigate = useNavigate()
  const [perfil, setPerfil] = useState(demoProfile)
  const [form, setForm] = useState(initialForm)
  const [mensaje, setMensaje] = useState('')
  const [error, setError] = useState('')
  const [cargando, setCargando] = useState(false)
  const [modoDemo, setModoDemo] = useState(false)
  const perfilEditadoRef = useRef(false)

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
        if (!perfilEditadoRef.current) {
          aplicarPerfil(response.data)
          setModoDemo(false)
        }
      } catch {
        if (!perfilEditadoRef.current) {
          aplicarPerfil(demoProfile)
          setModoDemo(true)
        }
      }
    }

    cargarPerfil()
  }, [])

  const handleChange = (event) => {
    perfilEditadoRef.current = true
    setForm({ ...form, [event.target.name]: event.target.value })
  }

  const construirPerfilDemo = () => ({
    ...perfil,
    nombreEmpresa: form.nombreEmpresa.trim(),
    nit: form.nit.trim(),
  })

  const handleSubmit = async (event) => {
    event.preventDefault()
    setMensaje('')
    setError('')
    setCargando(true)
    perfilEditadoRef.current = true

    try {
      const response = await client.put('/api/perfiles/despachador', {
        nombreEmpresa: form.nombreEmpresa.trim() || null,
        nit: form.nit.trim() || null,
      })
      aplicarPerfil(response.data)
      setModoDemo(false)
      setMensaje('Perfil de despachador guardado correctamente.')
    } catch (requestError) {
      if (requestError.response?.status === 401) {
        localStorage.removeItem('fleteco_token')
        localStorage.removeItem('fleteco_tipo_usuario')
        navigate('/')
        return
      }

      if (requestError.response?.data?.error) {
        setError(requestError.response.data.error)
      } else {
        aplicarPerfil(construirPerfilDemo())
        setModoDemo(true)
        setMensaje('Perfil guardado localmente mientras la API no está disponible.')
      }
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
            {modoDemo && <span className="status-chip">Demo</span>}
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
