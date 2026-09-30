import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import client from '../api/client'
import RouteMap from '../components/RouteMap'

function DetalleSolicitudPage() {
  const { id } = useParams()
  const navigate = useNavigate()

  const [solicitud, setSolicitud] = useState(null)
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState('')
  const [mensaje, setMensaje] = useState('')
  const [aceptando, setAceptando] = useState(false)

  const tipoUsuario = localStorage.getItem('fleteco_tipo_usuario')

  useEffect(() => {
    async function cargarSolicitud() {
      setCargando(true)
      setError('')

      try {
        const response = await client.get(`/api/solicitudes/${id}`)
        setSolicitud(response.data)
      } catch (err) {
        if (err.response?.status === 403) {
          setError('No tienes permiso para consultar esta solicitud.')
        } else if (err.response?.status === 404) {
          setError('La solicitud no existe.')
        } else {
          setError('No fue posible cargar la solicitud.')
        }
        setSolicitud(null)
      } finally {
        setCargando(false)
      }
    }

    cargarSolicitud()
  }, [id])

  const handleAceptar = async () => {
    setAceptando(true)
    setError('')
    setMensaje('')

    try {
      await client.post('/api/solicitudes/aceptar', { solicitudId: Number(id) })
      setMensaje('¡Flete aceptado con éxito!')
      // Recargar la solicitud para ver el nuevo estado
      const response = await client.get(`/api/solicitudes/${id}`)
      setSolicitud(response.data)
    } catch (err) {
      setError(err.response?.data?.error || 'No fue posible aceptar la solicitud.')
    } finally {
      setAceptando(false)
    }
  }

  const formatearFecha = (fechaStr) => {
    if (!fechaStr) return 'No especificada'
    const fecha = new Date(fechaStr)
    return fecha.toLocaleString('es-CO', {
      dateStyle: 'medium',
      timeStyle: 'short',
    })
  }

  if (cargando) {
    return (
      <main className="detalle-page">
        <p className="detalle-estado-carga">Cargando información del flete...</p>
      </main>
    )
  }

  if (error && !solicitud) {
    return (
      <main className="detalle-page">
        <div className="detalle-alerta detalle-alerta-error">{error}</div>
        <button type="button" className="detalle-volver" onClick={() => navigate(-1)}>
          ← Volver
        </button>
      </main>
    )
  }

  if (!solicitud) {
    return null
  }

  const puedeAceptar = tipoUsuario === 'CONDUCTOR' && solicitud.estado === 'PUBLICADA'

  return (
    <main className="detalle-page">
      <button type="button" className="detalle-volver" onClick={() => navigate('/perfil')}>
        ← Volver al perfil
      </button>

      <header className="detalle-hero">
        <div>
          <p className="eyebrow">Solicitud #{solicitud.id}</p>
          <h1>Detalle del flete</h1>
          <div className="detalle-hero-meta">
            <span className={`detalle-estado detalle-estado-${solicitud.estado?.toLowerCase()}`}>
              ● {solicitud.estado}
            </span>
            <span>Publicada el {formatearFecha(solicitud.fechaPublicacion)}</span>
          </div>
        </div>

        <div className="detalle-pago">
          <span>Pago ofrecido</span>
          <strong>${Number(solicitud.precioOfrecido).toLocaleString('es-CO')} COP</strong>
          {puedeAceptar && (
            <button
              type="button"
              className="btn-aceptar-flete detalle-btn-aceptar"
              onClick={handleAceptar}
              disabled={aceptando}
            >
              {aceptando ? 'Aceptando flete...' : '✓ Aceptar este flete'}
            </button>
          )}
        </div>
      </header>

      {mensaje && <div className="detalle-alerta detalle-alerta-ok">{mensaje}</div>}
      {error && <div className="detalle-alerta detalle-alerta-error">{error}</div>}

      <section className="detalle-panel">
        <h2 className="detalle-panel-titulo">📍 Recorrido</h2>
        <ol className="ruta-timeline">
          <li className="ruta-punto ruta-punto-origen">
            <span className="ruta-etiqueta">Origen · Recogida {formatearFecha(solicitud.fechaRecogida)}</span>
            <span className="ruta-lugar">{solicitud.origen}</span>
          </li>
          <li className="ruta-punto ruta-punto-destino">
            <span className="ruta-etiqueta">Destino · Entrega estimada {formatearFecha(solicitud.fechaEntregaEstimada)}</span>
            <span className="ruta-lugar">{solicitud.destino}</span>
          </li>
        </ol>
      </section>

      <div className="detalle-grid">
        <section className="detalle-panel">
          <h2 className="detalle-panel-titulo">📦 Especificaciones de la carga</h2>
          <dl className="detalle-datos">
            <div>
              <dt>Tipo de carga</dt>
              <dd>{solicitud.tipoCarga}</dd>
            </div>
            <div>
              <dt>Peso</dt>
              <dd>{Number(solicitud.peso).toLocaleString('es-CO')} toneladas</dd>
            </div>
            <div>
              <dt>Vehículo requerido</dt>
              <dd>{solicitud.tipoVehiculoRequerido}</dd>
            </div>
            <div>
              <dt>Cita en puerto</dt>
              <dd className={solicitud.requiereCitaPuerto ? 'detalle-dato-alerta' : ''}>
                {solicitud.requiereCitaPuerto
                  ? `⚓ ${solicitud.numeroCita ? `Cita #${solicitud.numeroCita}` : 'Requerida'}`
                  : 'No requiere'}
              </dd>
            </div>
          </dl>
        </section>

        <section className="detalle-panel">
          <h2 className="detalle-panel-titulo">📅 Cronograma</h2>
          <dl className="detalle-datos">
            <div>
              <dt>Fecha de recogida</dt>
              <dd>{formatearFecha(solicitud.fechaRecogida)}</dd>
            </div>
            <div>
              <dt>Entrega estimada</dt>
              <dd>{formatearFecha(solicitud.fechaEntregaEstimada)}</dd>
            </div>
            <div>
              <dt>Publicada</dt>
              <dd>{formatearFecha(solicitud.fechaPublicacion)}</dd>
            </div>
          </dl>
        </section>
      </div>

      <section className="detalle-panel">
        <h2 className="detalle-panel-titulo">🗺️ Ruta sugerida en el mapa</h2>
        <RouteMap
          origenLat={Number(solicitud.origenLat)}
          origenLng={Number(solicitud.origenLng)}
          destinoLat={Number(solicitud.destinoLat)}
          destinoLng={Number(solicitud.destinoLng)}
        />
      </section>
    </main>
  )
}

export default DetalleSolicitudPage
