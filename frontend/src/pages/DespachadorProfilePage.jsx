import { useCallback, useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import client from '../api/client'
import solicitudesClient from '../api/solicitudesClient'
import ProfileLayout from '../components/ProfileLayout'
import RequestMessage from '../components/RequestMessage'
import EstadoUbicacionConductor from '../components/EstadoUbicacionConductor'
import { INTERVALO_ENVIO_MS } from '../utils/geolocalizacion'

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

const PASOS_FLETE = [
  { estado: 'PUBLICADA', etiqueta: 'Publicado' },
  { estado: 'ACEPTADA', etiqueta: 'Aceptado' },
  { estado: 'EN_CURSO', etiqueta: 'En camino' },
  { estado: 'COMPLETADA', etiqueta: 'Entregado' },
]

const DESCRIPCION_ESTADO = {
  PUBLICADA: 'Esperando a que un conductor lo acepte',
  ACEPTADA: 'El conductor va a recoger la carga',
  EN_CURSO: 'La carga va en camino al destino',
  COMPLETADA: 'Entregado · confirma la recepción',
}

const formatearPesos = (valor) => `$${Number(valor || 0).toLocaleString('es-CO')} COP`

const formatearFecha = (fechaStr) => {
  if (!fechaStr) return 'No especificada'
  return new Date(fechaStr).toLocaleString('es-CO', { dateStyle: 'medium', timeStyle: 'short' })
}

function DespachadorProfilePage() {
  const navigate = useNavigate()
  const [perfil, setPerfil] = useState(emptyProfile)
  const [form, setForm] = useState(initialForm)
  const [mensaje, setMensaje] = useState('')
  const [error, setError] = useState('')
  const [cargando, setCargando] = useState(false)
  const [editandoEmpresa, setEditandoEmpresa] = useState(false)
  const [fletes, setFletes] = useState([])
  const [cargandoFletes, setCargandoFletes] = useState(false)
  const [confirmandoId, setConfirmandoId] = useState(null)

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
        const response = await client.get('/api/perfiles/despachador')
        setPerfil(response.data)
      } catch (requestError) {
        if (!cerrarSesionSiExpiro(requestError)) {
          setError('No fue posible cargar el perfil de despachador.')
        }
      }
    }

    cargarPerfil()
  }, [cerrarSesionSiExpiro])

  const cargarFletes = useCallback(async () => {
    setCargandoFletes(true)
    try {
      const response = await solicitudesClient.get('/api/solicitudes/mis-fletes')
      setFletes(response.data || [])
    } catch (requestError) {
      if (!cerrarSesionSiExpiro(requestError)) {
        setFletes([])
      }
    } finally {
      setCargandoFletes(false)
    }
  }, [cerrarSesionSiExpiro])

  useEffect(() => {
    cargarFletes()
  }, [cargarFletes])

  // Refresco silencioso para seguir la ubicación y el estado de los fletes activos.
  const hayViajesActivos = fletes.some((f) => ['ACEPTADA', 'EN_CURSO'].includes(f.estado))
  useEffect(() => {
    if (!hayViajesActivos) return undefined

    const intervalo = setInterval(async () => {
      try {
        const response = await solicitudesClient.get('/api/solicitudes/mis-fletes')
        setFletes(response.data || [])
      } catch {
        // Se conserva la última información; el siguiente intento vuelve a consultar.
      }
    }, INTERVALO_ENVIO_MS)

    return () => clearInterval(intervalo)
  }, [hayViajesActivos])

  const abrirEdicionEmpresa = () => {
    setForm({
      nombreEmpresa: perfil.nombreEmpresa || '',
      nit: perfil.nit || '',
    })
    setMensaje('')
    setError('')
    setEditandoEmpresa(true)
  }

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
      setPerfil(response.data)
      setEditandoEmpresa(false)
      setMensaje('Datos de la empresa guardados correctamente.')
    } catch (requestError) {
      if (cerrarSesionSiExpiro(requestError)) return
      setError(requestError.response?.data?.error || 'No fue posible guardar los datos de la empresa.')
    } finally {
      setCargando(false)
    }
  }

  const handleConfirmarEntrega = async (fleteId) => {
    if (!window.confirm('¿Confirmas que recibiste la carga? Esto liberará el pago al conductor.')) {
      return
    }

    setConfirmandoId(fleteId)
    setMensaje('')
    setError('')

    try {
      await solicitudesClient.patch(`/api/solicitudes/${fleteId}/confirmar-entrega`)
      setMensaje(`Recepción del flete #${fleteId} confirmada. El pago fue liberado al conductor.`)
      cargarFletes()
    } catch (requestError) {
      if (cerrarSesionSiExpiro(requestError)) return
      setError(requestError.response?.data?.error || 'No fue posible confirmar la entrega.')
    } finally {
      setConfirmandoId(null)
    }
  }

  const porConfirmar = fletes.filter((f) => f.estado === 'COMPLETADA').length

  return (
    <ProfileLayout
      role="Perfil de despachador"
      title="Mis fletes"
      subtitle="Sigue en tiempo real el estado de tus fletes y confirma las entregas."
    >
      <div className="profile-grid">
        <div className="conductor-main">
          <RequestMessage mensaje={mensaje} error={error} />

          {editandoEmpresa && (
            <section className="profile-panel">
              <div className="panel-heading">
                <h2>Datos de la empresa</h2>
                <button type="button" className="btn-actualizar" onClick={() => setEditandoEmpresa(false)}>
                  Cancelar
                </button>
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
                <button type="submit" disabled={cargando}>{cargando ? 'Guardando...' : 'Guardar cambios'}</button>
              </form>
            </section>
          )}

          <section className="profile-panel">
            <div className="panel-heading solicitudes-heading">
              <div>
                <h2>Fletes en seguimiento</h2>
                <small className="solicitudes-subtitulo">
                  {porConfirmar > 0
                    ? `Tienes ${porConfirmar} ${porConfirmar === 1 ? 'entrega' : 'entregas'} por confirmar`
                    : 'Publicados, en proceso o pendientes de confirmar'}
                </small>
              </div>
              <button
                type="button"
                className="btn-actualizar"
                onClick={cargarFletes}
                disabled={cargandoFletes}
              >
                {cargandoFletes ? 'Buscando...' : '↻ Actualizar'}
              </button>
            </div>

            {cargandoFletes && fletes.length === 0 ? (
              <p className="solicitudes-vacio">Cargando tus fletes...</p>
            ) : fletes.length > 0 ? (
              <div className="solicitudes-list">
                {fletes.map((flete) => {
                  const indicePaso = PASOS_FLETE.findIndex((paso) => paso.estado === flete.estado)
                  const puedeConfirmar = flete.estado === 'COMPLETADA' && flete.estadoPago === 'RETENIDO'

                  return (
                    <article
                      key={flete.id}
                      className={`solicitud-card-disponible ${puedeConfirmar ? 'flete-por-confirmar' : ''}`}
                    >
                      <div className="solicitud-header">
                        <div>
                          <strong className="solicitud-titulo">Solicitud #{flete.id}</strong>
                          <span className="solicitud-publicada">{DESCRIPCION_ESTADO[flete.estado]}</span>
                        </div>
                        <div className="solicitud-precio">
                          <span>{flete.estadoPago === 'RETENIDO' ? 'Pago retenido' : 'Pago ofrecido'}</span>
                          <strong>{formatearPesos(flete.precioOfrecido)}</strong>
                        </div>
                      </div>

                      <ol className="viaje-pasos viaje-pasos-4">
                        {PASOS_FLETE.map((paso, indice) => (
                          <li
                            key={paso.estado}
                            className={`viaje-paso ${indice < indicePaso ? 'hecho' : ''} ${indice === indicePaso ? 'actual' : ''}`}
                          >
                            <span className="viaje-paso-marca">{indice < indicePaso ? '✓' : indice + 1}</span>
                            <span>{paso.etiqueta}</span>
                          </li>
                        ))}
                      </ol>

                      <ol className="ruta-timeline">
                        <li className="ruta-punto ruta-punto-origen">
                          <span className="ruta-etiqueta">Origen · Recogida {formatearFecha(flete.fechaRecogida)}</span>
                          <span className="ruta-lugar">{flete.origen}</span>
                        </li>
                        <li className="ruta-punto ruta-punto-destino">
                          <span className="ruta-etiqueta">Destino · Entrega estimada {formatearFecha(flete.fechaEntregaEstimada)}</span>
                          <span className="ruta-lugar">{flete.destino}</span>
                        </li>
                      </ol>

                      <EstadoUbicacionConductor flete={flete} />

                      {flete.telefonoConductor && (
                        <div className="viaje-contacto">
                          <span>Conductor asignado</span>
                          <strong>{flete.nombreConductor}</strong>
                          <a href={`tel:${flete.telefonoConductor}`}>📞 {flete.telefonoConductor}</a>
                        </div>
                      )}

                      {puedeConfirmar && (
                        <p className="viaje-aviso">
                          El conductor reportó la entrega. Revisa que la carga llegó completa y confirma la recepción para liberar el pago.
                        </p>
                      )}

                      <div className="solicitud-actions">
                        <button
                          type="button"
                          className="btn-ver-detalle"
                          onClick={() => navigate(`/solicitudes/${flete.id}`)}
                        >
                          {['ACEPTADA', 'EN_CURSO'].includes(flete.estado)
                            ? '🗺️ Ver ubicación en el mapa'
                            : 'Ver detalles y mapa'}
                        </button>
                        {puedeConfirmar && (
                          <button
                            type="button"
                            className="btn-aceptar-flete"
                            disabled={confirmandoId === flete.id}
                            onClick={() => handleConfirmarEntrega(flete.id)}
                          >
                            {confirmandoId === flete.id ? 'Confirmando...' : '✓ Confirmar recepción'}
                          </button>
                        )}
                      </div>
                    </article>
                  )
                })}
              </div>
            ) : (
              <p className="solicitudes-vacio">
                No tienes fletes en seguimiento. Publica una nueva solicitud para empezar.
              </p>
            )}
          </section>
        </div>

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
            <button type="button" className="btn-link" onClick={abrirEdicionEmpresa}>
              Editar empresa
            </button>
          </div>

          <Link className="link-button" to="/solicitudes/nueva">+ Publicar nueva solicitud</Link>
        </aside>
      </div>
    </ProfileLayout>
  )
}

export default DespachadorProfilePage
