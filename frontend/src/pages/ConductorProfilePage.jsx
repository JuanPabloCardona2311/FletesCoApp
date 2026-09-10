import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import client from '../api/client'
import ProfileLayout from '../components/ProfileLayout'
import RequestMessage from '../components/RequestMessage'

const initialForm = {
  tipoVehiculo: '',
  placa: '',
  capacidadCarga: '',
  ubicacionLat: '',
  ubicacionLng: '',
}

const emptyProfile = {
  nombre: '',
  email: '',
  telefono: '',
  calificacionPromedio: 0,
  cancelacionesTotales: 0,
  ubicacionLat: '',
  ubicacionLng: '',
  vehiculos: [],
}

function ConductorProfilePage() {
  const navigate = useNavigate()
  const [perfil, setPerfil] = useState(emptyProfile)
  const [form, setForm] = useState(initialForm)
  const [mensaje, setMensaje] = useState('')
  const [error, setError] = useState('')
  const [cargando, setCargando] = useState(false)

  const vehiculoActivo = useMemo(
    () => perfil.vehiculos?.find((vehiculo) => vehiculo.activo) || perfil.vehiculos?.[0],
    [perfil.vehiculos],
  )

  const aplicarPerfil = (perfilActual) => {
    const activo = perfilActual.vehiculos?.find((vehiculo) => vehiculo.activo) || perfilActual.vehiculos?.[0]

    setPerfil(perfilActual)
    setForm({
      tipoVehiculo: activo?.tipoVehiculo || '',
      placa: activo?.placa || '',
      capacidadCarga: activo?.capacidadCarga?.toString() || '',
      ubicacionLat: perfilActual.ubicacionLat?.toString() || '',
      ubicacionLng: perfilActual.ubicacionLng?.toString() || '',
    })
  }

  useEffect(() => {
    const cargarPerfil = async () => {
      try {
        const response = await client.get('/api/perfiles/conductor')
        aplicarPerfil(response.data)
      } catch (requestError) {
        if (requestError.response?.status === 401) {
          localStorage.removeItem('fleteco_token')
          localStorage.removeItem('fleteco_tipo_usuario')
          navigate('/')
        } else {
          setError('No fue posible cargar el perfil de conductor.')
        }
      }
    }

    cargarPerfil()
  }, [navigate])

  const handleChange = (event) => {
    setForm({ ...form, [event.target.name]: event.target.value })
  }

  const construirPayload = () => ({
    tipoVehiculo: form.tipoVehiculo.trim(),
    placa: form.placa.trim(),
    capacidadCarga: Number(form.capacidadCarga),
    ubicacionLat: form.ubicacionLat ? Number(form.ubicacionLat) : null,
    ubicacionLng: form.ubicacionLng ? Number(form.ubicacionLng) : null,
  })

  const handleSubmit = async (event) => {
    event.preventDefault()
    setMensaje('')
    setError('')
    setCargando(true)

    try {
      const response = await client.put('/api/perfiles/conductor', construirPayload())
      aplicarPerfil(response.data)
      setMensaje('Perfil de conductor guardado correctamente.')
    } catch (requestError) {
      if (requestError.response?.status === 401) {
        localStorage.removeItem('fleteco_token')
        localStorage.removeItem('fleteco_tipo_usuario')
        navigate('/')
        return
      }

      setError(requestError.response?.data?.error || 'No fue posible guardar el perfil de conductor.')
    } finally {
      setCargando(false)
    }
  }

  return (
    <ProfileLayout role="Perfil de conductor" title="Vehículo y disponibilidad" subtitle="Datos mínimos para operar como conductor en FleteCo.">
      <div className="profile-grid">
        <section className="profile-panel">
          <div className="panel-heading">
            <h2>Datos del vehículo</h2>
          </div>

          <form onSubmit={handleSubmit}>
            <div className="two-columns">
              <label>
                Tipo de vehículo
                <input name="tipoVehiculo" value={form.tipoVehiculo} onChange={handleChange} placeholder="Camión, mula, camioneta" required />
              </label>
              <label>
                Placa
                <input name="placa" value={form.placa} onChange={handleChange} placeholder="ABC123" required maxLength="10" />
              </label>
            </div>

            <div className="two-columns">
              <label>
                Capacidad de carga
                <input name="capacidadCarga" type="number" min="0.01" step="0.01" value={form.capacidadCarga} onChange={handleChange} placeholder="8.5" required />
              </label>
              <label>
                Teléfono
                <input value={perfil.telefono || ''} readOnly />
              </label>
            </div>

            <div className="two-columns">
              <label>
                Latitud actual
                <input name="ubicacionLat" type="number" step="0.0000001" value={form.ubicacionLat} onChange={handleChange} placeholder="4.7110" />
              </label>
              <label>
                Longitud actual
                <input name="ubicacionLng" type="number" step="0.0000001" value={form.ubicacionLng} onChange={handleChange} placeholder="-74.0721" />
              </label>
            </div>

            <button type="submit" disabled={cargando}>{cargando ? 'Guardando...' : 'Guardar perfil'}</button>
            <RequestMessage mensaje={mensaje} error={error} />
          </form>
        </section>

        <aside className="summary-panel">
          <h2>{perfil.nombre}</h2>
          <p>{perfil.email}</p>

          <div className="metric-list">
            <div>
              <span>Calificación</span>
              <strong>{Number(perfil.calificacionPromedio || 0).toFixed(1)}</strong>
            </div>
            <div>
              <span>Cancelaciones</span>
              <strong>{perfil.cancelacionesTotales || 0}</strong>
            </div>
          </div>

          <div className="vehicle-card">
            <span>Vehículo activo</span>
            <strong>{vehiculoActivo ? `${vehiculoActivo.tipoVehiculo} - ${vehiculoActivo.placa}` : 'Sin vehículo'}</strong>
            <small>{vehiculoActivo?.estadoVerificacion || 'PENDIENTE'}</small>
          </div>
        </aside>
      </div>
    </ProfileLayout>
  )
}

export default ConductorProfilePage
