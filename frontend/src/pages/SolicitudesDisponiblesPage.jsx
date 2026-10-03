import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import perfilClient from '../api/perfilClient'
import solicitudesClient from '../api/solicitudesClient'
import ProfileLayout from '../components/ProfileLayout'
import RequestMessage from '../components/RequestMessage'
import { obtenerUbicacionActual } from '../utils/geolocalizacion'

const emptyProfile = {
  vehiculos: [],
}

const formatearFecha = (fechaStr) => {
  if (!fechaStr) return 'No especificada'
  return new Date(fechaStr).toLocaleString('es-CO', {
    dateStyle: 'medium',
    timeStyle: 'short',
  })
}

const formatearPesos = (valor) => `$${Number(valor || 0).toLocaleString('es-CO')} COP`

function SolicitudesDisponiblesPage() {
  const navigate = useNavigate()
  const [perfil, setPerfil] = useState(emptyProfile)
  const [solicitudes, setSolicitudes] = useState([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState('')
  const [aceptandoId, setAceptandoId] = useState(null)
  const [errorAceptar, setErrorAceptar] = useState({ id: null, mensaje: '' })
  const [etapaAceptar, setEtapaAceptar] = useState('')

  const cerrarSesionSiExpiro = useCallback((requestError) => {
    if (requestError.response?.status === 401) {
      localStorage.removeItem('fleteco_token')
      localStorage.removeItem('fleteco_tipo_usuario')
      navigate('/')
      return true
    }
    return false
  }, [navigate])

  useEffect(() => {
    const cargarPerfil = async () => {
      try {
        const response = await perfilClient.get('/api/perfiles/conductor')
        setPerfil(response.data)
      } catch (requestError) {
        if (!cerrarSesionSiExpiro(requestError)) {
          setError('No fue posible cargar tu perfil de conductor.')
        }
      }
    }

    cargarPerfil()
  }, [cerrarSesionSiExpiro])

  useEffect(() => {
    const cargarSolicitudes = async () => {
      setCargando(true)
      setError('')

      try {
        const response = await solicitudesClient.get('/api/solicitudes/disponibles')
        setSolicitudes(response.data || [])
      } catch (requestError) {
        if (!cerrarSesionSiExpiro(requestError)) {
          setError('No fue posible cargar las solicitudes disponibles.')
        }
        setSolicitudes([])
      } finally {
        setCargando(false)
      }
    }

    cargarSolicitudes()
  }, [cerrarSesionSiExpiro])

  const handleAceptarSolicitud = async (solicitudId) => {
    setAceptandoId(solicitudId)
    setError('')
    setErrorAceptar({ id: null, mensaje: '' })

    try {
      setEtapaAceptar('ubicacion')
      let ubicacion
      try {
        ubicacion = await obtenerUbicacionActual()
      } catch (errorUbicacion) {
        setErrorAceptar({
          id: solicitudId,
          mensaje: `${errorUbicacion.mensaje} Sin tu ubicación no puedes aceptar; la solicitud sigue disponible.`,
        })
        return
      }

      setEtapaAceptar('aceptando')
      await solicitudesClient.post('/api/solicitudes/aceptar', {
        solicitudId,
        ...ubicacion,
      })
      navigate('/perfil/conductor')
    } catch (requestError) {
      if (cerrarSesionSiExpiro(requestError)) return
      setErrorAceptar({
        id: solicitudId,
        mensaje: requestError.response?.data?.error || 'No fue posible aceptar la solicitud.',
      })
    } finally {
      setAceptandoId(null)
      setEtapaAceptar('')
    }
  }

  const vehiculoActivo = perfil.vehiculos?.find((vehiculo) => vehiculo.activo)
    || perfil.vehiculos?.[0]

  return (
    <ProfileLayout
      role="Perfil de conductor"
      title="Solicitudes disponibles"
      subtitle="Encuentra solicitudes compatibles con tu vehículo activo."
    >
      <div className="profile-grid">
        <main className="conductor-main">
          <RequestMessage mensaje="" error={error} />

          <section className="profile-panel">
            <div className="panel-heading solicitudes-heading">
              <div>
                <h2>Solicitudes disponibles para ti</h2>
                <small className="solicitudes-subtitulo">
                  Compatibles con tu vehículo {vehiculoActivo
                    ? `(${vehiculoActivo.tipoVehiculo} - ${vehiculoActivo.placa})`
                    : ''}
                </small>
              </div>
              <button
                type="button"
                className="btn-actualizar"
                onClick={() => window.location.reload()}
                disabled={cargando}
              >
                {cargando ? 'Buscando...' : '↻ Actualizar'}
              </button>
            </div>

            <p className="aviso-ubicacion">
              📍 Al aceptar una solicitud, tu navegador te pedirá permiso para usar tu ubicación.
              El despachador la verá mientras el flete esté activo.
            </p>

            {cargando ? (
              <p className="solicitudes-vacio">Buscando solicitudes compatibles...</p>
            ) : solicitudes.length > 0 ? (
              <div className="solicitudes-list">
                {solicitudes.map((solicitud) => (
                  <article key={solicitud.id} className="solicitud-card-disponible">
                    <div className="solicitud-header">
                      <div>
                        <strong className="solicitud-titulo">Solicitud #{solicitud.id}</strong>
                        <span className="solicitud-publicada">
                          Publicada el {new Date(solicitud.fechaPublicacion).toLocaleDateString('es-CO')}
                        </span>
                      </div>
                      <div className="solicitud-precio">
                        <span>Pago ofrecido</span>
                        <strong>{formatearPesos(solicitud.precioOfrecido)}</strong>
                      </div>
                    </div>

                    <ol className="ruta-timeline">
                      <li className="ruta-punto ruta-punto-origen">
                        <span className="ruta-etiqueta">Origen</span>
                        <span className="ruta-lugar">{solicitud.origen}</span>
                      </li>
                      <li className="ruta-punto ruta-punto-destino">
                        <span className="ruta-etiqueta">Destino</span>
                        <span className="ruta-lugar">{solicitud.destino}</span>
                      </li>
                    </ol>

                    <div className="solicitud-fechas">
                      <div>
                        <span>📅 Recogida</span>
                        <strong>{formatearFecha(solicitud.fechaRecogida)}</strong>
                      </div>
                      <div>
                        <span>🏁 Entrega estimada</span>
                        <strong>{formatearFecha(solicitud.fechaEntregaEstimada)}</strong>
                      </div>
                    </div>

                    <div className="solicitud-tags">
                      <span className="solicitud-tag">📦 Carga: {solicitud.tipoCarga}</span>
                      <span className="solicitud-tag">
                        ⚖️ Peso: {Number(solicitud.peso).toLocaleString('es-CO')} t
                      </span>
                      <span className="solicitud-tag">🚛 Requiere: {solicitud.tipoVehiculoRequerido}</span>
                      {solicitud.requiereCitaPuerto && (
                        <span className="solicitud-tag solicitud-tag-alerta">
                          ⚓ Requiere cita en puerto
                        </span>
                      )}
                    </div>

                    {errorAceptar.id === solicitud.id && (
                      <p className="viaje-aviso" role="alert">{errorAceptar.mensaje}</p>
                    )}

                    <div className="solicitud-actions">
                      <button
                        type="button"
                        className="btn-ver-detalle"
                        onClick={() => navigate(`/solicitudes/${solicitud.id}`)}
                      >
                        Ver detalles
                      </button>
                      <button
                        type="button"
                        className="btn-aceptar-flete"
                        disabled={aceptandoId !== null}
                        onClick={() => handleAceptarSolicitud(solicitud.id)}
                      >
                        {aceptandoId === solicitud.id
                          ? etapaAceptar === 'ubicacion'
                            ? '📍 Obteniendo ubicación...'
                            : 'Aceptando...'
                          : 'Aceptar solicitud'}
                      </button>
                    </div>
                  </article>
                ))}
              </div>
            ) : (
              <p className="solicitudes-vacio">
                {vehiculoActivo
                  ? 'No hay solicitudes disponibles compatibles con tu vehículo activo en este momento.'
                  : 'Agrega un vehículo para empezar a ver solicitudes compatibles.'}
              </p>
            )}
          </section>
        </main>

        <aside className="summary-panel">
          <h2>{perfil.nombre}</h2>
          <p>{perfil.email}</p>
          <div className="vehicle-card">
            <span>Vehículo activo para hoy</span>
            <strong>{vehiculoActivo
              ? `${vehiculoActivo.tipoVehiculo} - ${vehiculoActivo.placa}`
              : 'Sin vehículo activo'}</strong>
            <small>{vehiculoActivo
              ? `Capacidad: ${vehiculoActivo.capacidadCarga} t • ${vehiculoActivo.estadoVerificacion || 'PENDIENTE'}`
              : 'Agrega uno para empezar'}</small>
          </div>
          <button
            type="button"
            className="link-button"
            onClick={() => navigate('/perfil/conductor')}
          >
            Volver a mi perfil
          </button>
        </aside>
      </div>
    </ProfileLayout>
  )
}

export default SolicitudesDisponiblesPage
