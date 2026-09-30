import { useCallback, useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import client from '../api/client'
import ProfileLayout from '../components/ProfileLayout'
import RequestMessage from '../components/RequestMessage'
import { avisarViajeActualizado, obtenerUbicacionActual } from '../utils/geolocalizacion'

const initialForm = {
  tipoVehiculo: '',
  placa: '',
  capacidadCarga: '',
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

const PASOS_VIAJE = [
  { estado: 'ACEPTADA', etiqueta: 'Aceptado' },
  { estado: 'EN_CURSO', etiqueta: 'En camino' },
  { estado: 'COMPLETADA', etiqueta: 'Entregado' },
]

const formatearFecha = (fechaStr) => {
  if (!fechaStr) return 'No especificada'
  return new Date(fechaStr).toLocaleString('es-CO', { dateStyle: 'medium', timeStyle: 'short' })
}

const formatearPesos = (valor) => `$${Number(valor || 0).toLocaleString('es-CO')} COP`

function ConductorProfilePage() {
  const navigate = useNavigate()
  const [perfil, setPerfil] = useState(emptyProfile)
  const [form, setForm] = useState(initialForm)
  const [mensaje, setMensaje] = useState('')
  const [error, setError] = useState('')
  const [cargando, setCargando] = useState(false)
  const [cargandoActivacion, setCargandoActivacion] = useState(null)
  const [viajeActual, setViajeActual] = useState(null)
  const [avanzandoViaje, setAvanzandoViaje] = useState(false)
  const [modoVehiculo, setModoVehiculo] = useState(null)
  const [solicitudesDisponibles, setSolicitudesDisponibles] = useState([])
  const [cargandoDisponibles, setCargandoDisponibles] = useState(false)
  const [aceptandoId, setAceptandoId] = useState(null)
  const [etapaAceptar, setEtapaAceptar] = useState('')
  const [errorAceptar, setErrorAceptar] = useState({ id: null, mensaje: '' })

  const vehiculoActivo = useMemo(
    () => perfil.vehiculos?.find((vehiculo) => vehiculo.activo) || perfil.vehiculos?.[0],
    [perfil.vehiculos],
  )

  const cerrarSesionSiExpiro = (requestError) => {
    if (requestError.response?.status === 401) {
      localStorage.removeItem('fleteco_token')
      localStorage.removeItem('fleteco_tipo_usuario')
      navigate('/')
      return true
    }
    return false
  }

  useEffect(() => {
    const cargarPerfil = async () => {
      try {
        const response = await client.get('/api/perfiles/conductor')
        setPerfil(response.data)
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

  const cargarSolicitudesDisponibles = useCallback(async () => {
    setCargandoDisponibles(true)
    try {
      const response = await client.get('/api/solicitudes/disponibles')
      setSolicitudesDisponibles(response.data || [])
    } catch {
      setSolicitudesDisponibles([])
    } finally {
      setCargandoDisponibles(false)
    }
  }, [])

  useEffect(() => {
    cargarSolicitudesDisponibles()
  }, [cargarSolicitudesDisponibles])

  useEffect(() => {
    const cargarViajeActual = async () => {
      try {
        const response = await client.get('/api/solicitudes/aceptada')
        setViajeActual(response.data)
      } catch (requestError) {
        if (requestError.response?.status === 401) {
          localStorage.removeItem('fleteco_token')
          localStorage.removeItem('fleteco_tipo_usuario')
          navigate('/')
        } else {
          setViajeActual(null)
        }
      }
    }

    cargarViajeActual()
  }, [navigate])

  const abrirFormularioVehiculo = (modo) => {
    const base = modo === 'editar' ? vehiculoActivo : null
    setForm({
      tipoVehiculo: base?.tipoVehiculo || '',
      placa: base?.placa || '',
      capacidadCarga: base?.capacidadCarga?.toString() || '',
    })
    setMensaje('')
    setError('')
    setModoVehiculo(modo)
  }

  const handleChange = (event) => {
    setForm({ ...form, [event.target.name]: event.target.value })
  }

  const construirPayload = () => ({
    tipoVehiculo: form.tipoVehiculo.trim(),
    placa: form.placa.trim(),
    capacidadCarga: Number(form.capacidadCarga),
  })

  const handleSubmit = async (event) => {
    event.preventDefault()
    setMensaje('')
    setError('')
    setCargando(true)

    try {
      const response = await client.put('/api/perfiles/conductor', construirPayload())
      setPerfil(response.data)
      cargarSolicitudesDisponibles()
      setMensaje(modoVehiculo === 'agregar'
        ? 'Vehículo agregado y activado para la jornada.'
        : 'Vehículo actualizado correctamente.')
      setModoVehiculo(null)
    } catch (requestError) {
      if (cerrarSesionSiExpiro(requestError)) return
      setError(requestError.response?.data?.error || 'No fue posible guardar el vehículo.')
    } finally {
      setCargando(false)
    }
  }

  const handleActivarVehiculo = async (vehiculoId) => {
    setMensaje('')
    setError('')
    setCargandoActivacion(vehiculoId)

    try {
      const response = await client.put(`/api/perfiles/conductor/vehiculos/${vehiculoId}/activar`)
      setPerfil(response.data)
      cargarSolicitudesDisponibles()
      setMensaje('Vehículo activado correctamente para la jornada.')
    } catch (requestError) {
      if (cerrarSesionSiExpiro(requestError)) return
      setError(requestError.response?.data?.error || 'No fue posible activar el vehículo.')
    } finally {
      setCargandoActivacion(null)
    }
  }

  const handleAceptarSolicitud = async (solicitudId) => {
    setAceptandoId(solicitudId)
    setMensaje('')
    setError('')
    setErrorAceptar({ id: null, mensaje: '' })

    try {
      // Primero el permiso de ubicación: si no se concede, la solicitud sigue disponible para otros.
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
      await client.post('/api/solicitudes/aceptar', { solicitudId, ...ubicacion })
      const responseViaje = await client.get('/api/solicitudes/aceptada')
      setViajeActual(responseViaje.data)
      avisarViajeActualizado()
      setMensaje('¡Solicitud aceptada! Este es ahora tu viaje actual.')
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

  const handleAvanzarViaje = async () => {
    if (!viajeActual) return

    const esEntrega = viajeActual.estado === 'EN_CURSO'
    if (esEntrega && !window.confirm('¿Confirmas que entregaste la carga en el destino?')) {
      return
    }

    setAvanzandoViaje(true)
    setMensaje('')
    setError('')

    try {
      const accion = esEntrega ? 'entregar' : 'iniciar'
      const response = await client.patch(`/api/solicitudes/${viajeActual.id}/${accion}`)
      avisarViajeActualizado()

      if (response.data.estado === 'COMPLETADA') {
        setViajeActual(null)
        cargarSolicitudesDisponibles()
        setMensaje('Entrega registrada. Tu pago se liberará cuando el despachador confirme la recepción.')
      } else {
        setViajeActual(response.data)
        setMensaje('Viaje iniciado. ¡Buen camino!')
      }
    } catch (requestError) {
      if (cerrarSesionSiExpiro(requestError)) return
      setError(requestError.response?.data?.error || 'No fue posible actualizar el estado del viaje.')
    } finally {
      setAvanzandoViaje(false)
    }
  }

  const indicePasoActual = viajeActual
    ? PASOS_VIAJE.findIndex((paso) => paso.estado === viajeActual.estado)
    : -1

  return (
    <ProfileLayout
      role="Perfil de conductor"
      title={viajeActual ? 'Tu viaje actual' : 'Fletes disponibles'}
      subtitle={viajeActual
        ? 'Sigue los pasos para completar la entrega y recibir tu pago.'
        : 'Encuentra solicitudes compatibles con tu vehículo activo.'}
    >
      <div className="profile-grid">
        <div className="conductor-main">
          <RequestMessage mensaje={mensaje} error={error} />

          {modoVehiculo && (
            <section className="profile-panel">
              <div className="panel-heading">
                <h2>{modoVehiculo === 'agregar' ? 'Agregar nuevo vehículo' : 'Editar vehículo activo'}</h2>
                <button type="button" className="btn-actualizar" onClick={() => setModoVehiculo(null)}>
                  Cancelar
                </button>
              </div>

              <form onSubmit={handleSubmit}>
                <div className="two-columns">
                  <label>
                    Tipo de vehículo
                    <input name="tipoVehiculo" value={form.tipoVehiculo} onChange={handleChange} placeholder="Camión, mula, camioneta" required />
                  </label>
                  <label>
                    Placa
                    <input
                      name="placa"
                      value={form.placa}
                      onChange={handleChange}
                      placeholder="ABC123"
                      required
                      maxLength="10"
                      readOnly={modoVehiculo === 'editar'}
                    />
                  </label>
                </div>

                <div className="two-columns">
                  <label>
                    Capacidad de carga (toneladas)
                    <input name="capacidadCarga" type="number" min="0.01" step="0.01" value={form.capacidadCarga} onChange={handleChange} placeholder="8.5" required />
                  </label>
                  <label>
                    Teléfono
                    <input value={perfil.telefono || ''} readOnly />
                  </label>
                </div>

                <button type="submit" disabled={cargando}>
                  {cargando ? 'Guardando...' : modoVehiculo === 'agregar' ? 'Agregar y activar vehículo' : 'Guardar cambios'}
                </button>
              </form>
            </section>
          )}

          {viajeActual ? (
            <section className="profile-panel viaje-panel">
              <div className="solicitud-header">
                <div>
                  <p className="eyebrow">Mi viaje actual · Solicitud #{viajeActual.id}</p>
                  <strong className="solicitud-titulo">
                    {viajeActual.estado === 'ACEPTADA' ? 'Dirígete a recoger la carga' : 'Carga en camino al destino'}
                  </strong>
                </div>
                <div className="solicitud-precio">
                  <span>{viajeActual.montoNetoConductor != null ? 'Recibirás' : 'Pago ofrecido'}</span>
                  <strong>{formatearPesos(viajeActual.montoNetoConductor ?? viajeActual.precioOfrecido)}</strong>
                </div>
              </div>

              <ol className="viaje-pasos">
                {PASOS_VIAJE.map((paso, indice) => (
                  <li
                    key={paso.estado}
                    className={`viaje-paso ${indice < indicePasoActual ? 'hecho' : ''} ${indice === indicePasoActual ? 'actual' : ''}`}
                  >
                    <span className="viaje-paso-marca">{indice < indicePasoActual ? '✓' : indice + 1}</span>
                    <span>{paso.etiqueta}</span>
                  </li>
                ))}
              </ol>

              <ol className="ruta-timeline">
                <li className="ruta-punto ruta-punto-origen">
                  <span className="ruta-etiqueta">Recogida · {formatearFecha(viajeActual.fechaRecogida)}</span>
                  <span className="ruta-lugar">{viajeActual.origen}</span>
                </li>
                <li className="ruta-punto ruta-punto-destino">
                  <span className="ruta-etiqueta">Entrega estimada · {formatearFecha(viajeActual.fechaEntregaEstimada)}</span>
                  <span className="ruta-lugar">{viajeActual.destino}</span>
                </li>
              </ol>

              <div className="solicitud-tags">
                <span className="solicitud-tag">📦 Carga: {viajeActual.tipoCarga}</span>
                <span className="solicitud-tag">⚖️ Peso: {Number(viajeActual.peso).toLocaleString('es-CO')} t</span>
                {viajeActual.requiereCitaPuerto && (
                  <span className="solicitud-tag solicitud-tag-alerta">
                    ⚓ Cita en puerto{viajeActual.numeroCita ? ` #${viajeActual.numeroCita}` : ''}
                  </span>
                )}
              </div>

              {viajeActual.telefonoDespachador && (
                <div className="viaje-contacto">
                  <span>Contacto del despachador</span>
                  <strong>{viajeActual.nombreDespachador}</strong>
                  <a href={`tel:${viajeActual.telefonoDespachador}`}>📞 {viajeActual.telefonoDespachador}</a>
                </div>
              )}

              <div className="solicitud-actions">
                <button
                  type="button"
                  className="btn-ver-detalle"
                  onClick={() => navigate(`/solicitudes/${viajeActual.id}`)}
                >
                  Ver detalles y mapa
                </button>
                <button
                  type="button"
                  className="btn-aceptar-flete"
                  disabled={avanzandoViaje}
                  onClick={handleAvanzarViaje}
                >
                  {avanzandoViaje
                    ? 'Actualizando...'
                    : viajeActual.estado === 'ACEPTADA' ? '🚚 Iniciar viaje' : '✓ Marcar como entregado'}
                </button>
              </div>
            </section>
          ) : (
            <section className="profile-panel">
              <div className="panel-heading solicitudes-heading">
                <div>
                  <h2>Solicitudes disponibles para ti</h2>
                  <small className="solicitudes-subtitulo">
                    Compatibles con tu vehículo {vehiculoActivo ? `(${vehiculoActivo.tipoVehiculo} - ${vehiculoActivo.placa})` : ''}
                  </small>
                </div>
                <button
                  type="button"
                  className="btn-actualizar"
                  onClick={cargarSolicitudesDisponibles}
                  disabled={cargandoDisponibles}
                >
                  {cargandoDisponibles ? 'Buscando...' : '↻ Actualizar'}
                </button>
              </div>

              <p className="aviso-ubicacion">
                📍 Al aceptar una solicitud, tu navegador te pedirá permiso para usar tu ubicación.
                El despachador la verá mientras el flete esté activo (desde 24 h antes de la recogida hasta la entrega).
              </p>

              {cargandoDisponibles ? (
                <p className="solicitudes-vacio">Buscando solicitudes compatibles...</p>
              ) : solicitudesDisponibles.length > 0 ? (
                <div className="solicitudes-list">
                  {solicitudesDisponibles.map((s) => (
                    <article key={s.id} className="solicitud-card-disponible">
                      <div className="solicitud-header">
                        <div>
                          <strong className="solicitud-titulo">Solicitud #{s.id}</strong>
                          <span className="solicitud-publicada">
                            Publicada el {new Date(s.fechaPublicacion).toLocaleDateString('es-CO')}
                          </span>
                        </div>
                        <div className="solicitud-precio">
                          <span>Pago ofrecido</span>
                          <strong>{formatearPesos(s.precioOfrecido)}</strong>
                        </div>
                      </div>

                      <ol className="ruta-timeline">
                        <li className="ruta-punto ruta-punto-origen">
                          <span className="ruta-etiqueta">Origen</span>
                          <span className="ruta-lugar">{s.origen}</span>
                        </li>
                        <li className="ruta-punto ruta-punto-destino">
                          <span className="ruta-etiqueta">Destino</span>
                          <span className="ruta-lugar">{s.destino}</span>
                        </li>
                      </ol>

                      <div className="solicitud-fechas">
                        <div>
                          <span>📅 Recogida</span>
                          <strong>{formatearFecha(s.fechaRecogida)}</strong>
                        </div>
                        <div>
                          <span>🏁 Entrega estimada</span>
                          <strong>{formatearFecha(s.fechaEntregaEstimada)}</strong>
                        </div>
                      </div>

                      <div className="solicitud-tags">
                        <span className="solicitud-tag">📦 Carga: {s.tipoCarga}</span>
                        <span className="solicitud-tag">⚖️ Peso: {Number(s.peso).toLocaleString('es-CO')} t</span>
                        <span className="solicitud-tag">🚛 Requiere: {s.tipoVehiculoRequerido}</span>
                        {s.requiereCitaPuerto && (
                          <span className="solicitud-tag solicitud-tag-alerta">⚓ Requiere cita en puerto</span>
                        )}
                      </div>

                      {errorAceptar.id === s.id && (
                        <p className="viaje-aviso" role="alert">{errorAceptar.mensaje}</p>
                      )}

                      <div className="solicitud-actions">
                        <button
                          type="button"
                          className="btn-ver-detalle"
                          onClick={() => navigate(`/solicitudes/${s.id}`)}
                        >
                          Ver detalles
                        </button>
                        <button
                          type="button"
                          className="btn-aceptar-flete"
                          disabled={aceptandoId !== null}
                          onClick={() => handleAceptarSolicitud(s.id)}
                        >
                          {aceptandoId === s.id
                            ? etapaAceptar === 'ubicacion' ? '📍 Obteniendo ubicación...' : 'Aceptando...'
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
          )}
        </div>

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
            <span>Vehículo activo para hoy</span>
            <strong>{vehiculoActivo ? `${vehiculoActivo.tipoVehiculo} - ${vehiculoActivo.placa}` : 'Sin vehículo activo'}</strong>
            <small>{vehiculoActivo ? `Capacidad: ${vehiculoActivo.capacidadCarga} t • ${vehiculoActivo.estadoVerificacion || 'PENDIENTE'}` : 'Agrega uno para empezar'}</small>
            {vehiculoActivo && (
              <button type="button" className="btn-link" onClick={() => abrirFormularioVehiculo('editar')}>
                Editar vehículo
              </button>
            )}
          </div>

          <div className="vehiculos-seccion">
            <div className="vehiculos-seccion-heading">
              <h3>Mis vehículos ({perfil.vehiculos?.length || 0})</h3>
              <button
                type="button"
                className="btn-agregar-vehiculo"
                disabled={Boolean(viajeActual)}
                title={viajeActual ? 'No puedes cambiar de vehículo durante un viaje' : undefined}
                onClick={() => abrirFormularioVehiculo('agregar')}
              >
                + Agregar
              </button>
            </div>

            {perfil.vehiculos && perfil.vehiculos.length > 0 ? (
              <div className="vehiculos-registrados-list">
                {perfil.vehiculos.map((v) => (
                  <div key={v.id} className={`vehiculo-item ${v.activo ? 'activo' : ''}`}>
                    <div className="vehiculo-item-info">
                      <strong style={{ fontSize: '0.9rem' }}>{v.placa}</strong>
                      <span style={{ fontSize: '0.78rem', color: 'var(--muted)' }}>
                        {v.tipoVehiculo} • {v.capacidadCarga} t
                      </span>
                    </div>
                    {v.activo ? (
                      <span className="badge-activo">✓ En uso</span>
                    ) : (
                      <button
                        type="button"
                        className="btn-activar-vehiculo"
                        disabled={cargandoActivacion === v.id || cargando || Boolean(viajeActual)}
                        onClick={() => handleActivarVehiculo(v.id)}
                      >
                        {cargandoActivacion === v.id ? 'Activando...' : 'Activar'}
                      </button>
                    )}
                  </div>
                ))}
              </div>
            ) : (
              <p style={{ fontSize: '0.8rem', color: 'var(--muted)' }}>No tienes vehículos registrados.</p>
            )}

            {viajeActual && (
              <p className="vehiculos-nota">No puedes cambiar de vehículo mientras tengas un viaje activo.</p>
            )}
          </div>
        </aside>
      </div>
    </ProfileLayout>
  )
}

export default ConductorProfilePage
