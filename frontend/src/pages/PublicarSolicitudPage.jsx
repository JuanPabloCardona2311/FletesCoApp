import { useState, useRef, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import client from '../api/client'
import ProfileLayout from '../components/ProfileLayout'
import RequestMessage from '../components/RequestMessage'
import RouteMap from '../components/RouteMap'
import SelectorRutaMap from '../components/SelectorRutaMap'

// --- estado inicial del formulario — siempre vacío al montar ---
const initialForm = {
  origen: '',
  destino: '',
  tipoCarga: '',
  tipoVehiculoRequerido: '',
  peso: '',
  precioOfrecido: '',
  fechaRecogida: '',
  fechaEntregaEstimada: '',
  requiereCitaPuerto: false,
  numeroCita: '',
}

// --- geocodificación inversa con Nominatim ---
async function geocodificarInverso(lat, lng, signal) {
  const url =
    `https://nominatim.openstreetmap.org/reverse?format=jsonv2` +
    `&lat=${lat}&lon=${lng}&accept-language=es`
  const res = await fetch(url, {
    headers: { 'Accept-Language': 'es' },
    signal,
  })
  if (!res.ok) throw new Error('Nominatim no disponible')
  const data = await res.json()
  return data.display_name || `${lat.toFixed(5)}, ${lng.toFixed(5)}`
}

function PublicarSolicitudPage() {
  const navigate = useNavigate()

  const [form, setForm] = useState(initialForm)
  const [errors, setErrors] = useState({})
  const [mensaje, setMensaje] = useState('')
  const [errorGlobal, setErrorGlobal] = useState('')
  const [cargando, setCargando] = useState(false)

  // puntos del mapa: { lat, lng } | null
  const [puntoOrigen, setPuntoOrigen] = useState(null)
  const [puntoDestino, setPuntoDestino] = useState(null)
  const [modoMapa, setModoMapa] = useState('ORIGEN')

  // Contador por tipo para descartar respuestas obsoletas de Nominatim
  const geoSeqRef = useRef({ ORIGEN: 0, DESTINO: 0 })
  // Timeout pendiente por tipo para throttle de Nominatim
  const geoTimeoutRef = useRef({ ORIGEN: null, DESTINO: null })

  // ---------- handlers de formulario ----------

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target
    setForm((prev) => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value,
    }))
    if (errors[name]) setErrors((prev) => ({ ...prev, [name]: undefined }))
  }

  // ---------- handler del mapa ----------
  // Fija el punto de inmediato con las coordenadas como texto,
  // luego actualiza en segundo plano cuando responda Nominatim.
  // Descarta respuestas obsoletas comparando el contador de secuencia.

  const handleSeleccionar = (tipo, lat, lng) => {
    // 1. Fija el punto y el texto de coordenadas de inmediato
    const textoInicial = `${lat.toFixed(5)}, ${lng.toFixed(5)}`

    if (tipo === 'ORIGEN') {
      setPuntoOrigen({ lat, lng })
      setForm((prev) => ({ ...prev, origen: textoInicial }))
      if (errors.origen || errors.puntoOrigen)
        setErrors((prev) => ({ ...prev, origen: undefined, puntoOrigen: undefined }))
    } else {
      setPuntoDestino({ lat, lng })
      setForm((prev) => ({ ...prev, destino: textoInicial }))
      if (errors.destino || errors.puntoDestino)
        setErrors((prev) => ({ ...prev, destino: undefined, puntoDestino: undefined }))
    }

    // 2. Cancela cualquier llamada a Nominatim pendiente para este tipo
    if (geoTimeoutRef.current[tipo] !== null) {
      clearTimeout(geoTimeoutRef.current[tipo])
    }

    // 3. Incrementa el contador de secuencia para este tipo
    geoSeqRef.current[tipo] += 1
    const miSeq = geoSeqRef.current[tipo]

    // 4. Programa la llamada a Nominatim con 1000 ms de retardo (throttle)
    geoTimeoutRef.current[tipo] = setTimeout(() => {
      geoTimeoutRef.current[tipo] = null
      geocodificarInverso(lat, lng)
        .then((texto) => {
          if (geoSeqRef.current[tipo] !== miSeq) return
          if (tipo === 'ORIGEN') {
            setForm((prev) => ({ ...prev, origen: texto }))
          } else {
            setForm((prev) => ({ ...prev, destino: texto }))
          }
        })
        .catch(() => {
          // fallo silencioso; el texto de coordenadas ya está puesto
        })
    }, 1000)
  }

  // Limpia timeouts pendientes al desmontar el componente
  useEffect(() => {
    return () => {
      clearTimeout(geoTimeoutRef.current.ORIGEN)
      clearTimeout(geoTimeoutRef.current.DESTINO)
    }
  }, [])

  const handleLimpiar = (tipo) => {
    if (tipo === 'ORIGEN') {
      setPuntoOrigen(null)
      setForm((prev) => ({ ...prev, origen: '' }))
      setModoMapa('ORIGEN')
    } else {
      setPuntoDestino(null)
      setForm((prev) => ({ ...prev, destino: '' }))
    }
  }

  // ---------- validación cliente ----------

  const validar = () => {
    const e = {}
    const ahora = new Date()

    if (!form.origen.trim()) e.origen = 'El origen es obligatorio.'
    else if (form.origen.length > 200) e.origen = 'Máximo 200 caracteres.'

    if (!form.destino.trim()) e.destino = 'El destino es obligatorio.'
    else if (form.destino.length > 200) e.destino = 'Máximo 200 caracteres.'

    if (!puntoOrigen) e.puntoOrigen = 'Marca el origen en el mapa.'
    if (!puntoDestino) e.puntoDestino = 'Marca el destino en el mapa.'

    if (!form.tipoCarga.trim()) e.tipoCarga = 'El tipo de carga es obligatorio.'
    else if (form.tipoCarga.length > 100) e.tipoCarga = 'Máximo 100 caracteres.'

    if (!form.tipoVehiculoRequerido) e.tipoVehiculoRequerido = 'Selecciona un tipo de vehículo.'

    if (!form.peso) e.peso = 'El peso es obligatorio.'
    else if (Number(form.peso) <= 0) e.peso = 'El peso debe ser mayor a 0.'

    if (!form.precioOfrecido) e.precioOfrecido = 'El precio es obligatorio.'
    else if (Number(form.precioOfrecido) <= 0) e.precioOfrecido = 'El precio debe ser mayor a 0.'

    if (!form.fechaRecogida) {
      e.fechaRecogida = 'La fecha de recogida es obligatoria.'
    } else if (new Date(form.fechaRecogida) <= ahora) {
      e.fechaRecogida = 'La fecha de recogida debe ser futura.'
    }

    if (!form.fechaEntregaEstimada) {
      e.fechaEntregaEstimada = 'La fecha de entrega estimada es obligatoria.'
    } else if (
      form.fechaRecogida &&
      new Date(form.fechaEntregaEstimada) <= new Date(form.fechaRecogida)
    ) {
      e.fechaEntregaEstimada = 'La fecha de entrega debe ser posterior a la de recogida.'
    }

    if (form.requiereCitaPuerto) {
      if (!form.numeroCita.trim()) e.numeroCita = 'El número de cita es obligatorio.'
      else if (form.numeroCita.length > 50) e.numeroCita = 'Máximo 50 caracteres.'
    }

    return e
  }

  // ---------- submit ----------

  const handleSubmit = async (e) => {
    e.preventDefault()
    setMensaje('')
    setErrorGlobal('')

    const validationErrors = validar()
    if (Object.keys(validationErrors).length > 0) {
      setErrors(validationErrors)
      return
    }

    setCargando(true)
    try {
      const payload = {
        origen: form.origen.trim(),
        destino: form.destino.trim(),
        origenLat: puntoOrigen.lat,
        origenLng: puntoOrigen.lng,
        destinoLat: puntoDestino.lat,
        destinoLng: puntoDestino.lng,
        tipoCarga: form.tipoCarga.trim(),
        tipoVehiculoRequerido: form.tipoVehiculoRequerido,
        peso: Number(form.peso),
        precioOfrecido: Number(form.precioOfrecido),
        fechaRecogida: form.fechaRecogida,
        fechaEntregaEstimada: form.fechaEntregaEstimada,
        requiereCitaPuerto: form.requiereCitaPuerto,
        numeroCita: form.requiereCitaPuerto ? form.numeroCita.trim() || null : null,
      }

      const response = await client.post('/api/solicitudes', payload)
      navigate(`/solicitudes/${response.data.id}`)
    } catch (requestError) {
      const status = requestError.response?.status
      const data = requestError.response?.data

      if (status === 401) {
        localStorage.removeItem('fleteco_token')
        localStorage.removeItem('fleteco_tipo_usuario')
        navigate('/')
        return
      }

      if (status === 403) {
        setErrorGlobal('No tienes permiso para realizar esta acción.')
        return
      }

      if (status === 400) {
        if (data?.errores) {
          // Separa errores de campos conocidos de los que no se renderizan
          const camposConocidos = [
            'origen', 'destino', 'tipoCarga', 'tipoVehiculoRequerido',
            'peso', 'precioOfrecido', 'fechaRecogida', 'fechaEntregaEstimada',
            'numeroCita', 'origenLat', 'origenLng', 'destinoLat', 'destinoLng',
          ]
          const erroresCampo = {}
          const erroresHuerfanos = []
          Object.entries(data.errores).forEach(([campo, msg]) => {
            if (camposConocidos.includes(campo)) {
              erroresCampo[campo] = msg
            } else {
              erroresHuerfanos.push(msg)
            }
          })
          if (Object.keys(erroresCampo).length > 0) setErrors(erroresCampo)
          if (erroresHuerfanos.length > 0)
            setErrorGlobal(erroresHuerfanos.join(' '))
        } else if (data?.error) {
          setErrorGlobal(data.error)
        } else {
          setErrorGlobal('Los datos enviados no son válidos.')
        }
        return
      }

      setErrorGlobal('No fue posible conectar con el servidor. Intenta de nuevo.')
    } finally {
      setCargando(false)
    }
  }

  // ---------- render ----------

  const ambosPuntosListos = puntoOrigen && puntoDestino

  return (
    <ProfileLayout
      role="Perfil de despachador"
      title="Publicar solicitud de flete"
      subtitle="Indica origen, destino y condiciones del servicio. Los datos no se precargan del perfil."
    >
      <div className="profile-panel">
        <form onSubmit={handleSubmit} noValidate>

          {/* ── Sección: Mapa de selección ── */}
          <h2>Selecciona origen y destino en el mapa</h2>

          <SelectorRutaMap
            origen={puntoOrigen}
            destino={puntoDestino}
            modo={modoMapa}
            onModoChange={setModoMapa}
            onSeleccionar={handleSeleccionar}
            onLimpiar={handleLimpiar}
          />

          {errors.puntoOrigen && (
            <span className="field-error" role="alert">{errors.puntoOrigen}</span>
          )}
          {errors.puntoDestino && (
            <span className="field-error" role="alert">{errors.puntoDestino}</span>
          )}

          {/* ── Sección: Textos de origen y destino ── */}
          <div className="two-columns">
            <label>
              Origen
              <input
                name="origen"
                value={form.origen}
                onChange={handleChange}
                placeholder="Ciudad o dirección de recogida"
                maxLength="200"
              />
              {errors.origen && (
                <span className="field-error" role="alert">{errors.origen}</span>
              )}
            </label>

            <label>
              Destino
              <input
                name="destino"
                value={form.destino}
                onChange={handleChange}
                placeholder="Ciudad o dirección de entrega"
                maxLength="200"
              />
              {errors.destino && (
                <span className="field-error" role="alert">{errors.destino}</span>
              )}
            </label>
          </div>

          {/* ── Vista previa de ruta ── */}
          {ambosPuntosListos && (
            <div className="publicar-solicitud__ruta-preview">
              <h3>Vista previa de la ruta</h3>
              <RouteMap
                origenLat={puntoOrigen.lat}
                origenLng={puntoOrigen.lng}
                destinoLat={puntoDestino.lat}
                destinoLng={puntoDestino.lng}
              />
            </div>
          )}

          {/* ── Sección: Datos de la carga ── */}
          <h2>Datos de la carga</h2>

          <label>
            Tipo de carga
            <input
              name="tipoCarga"
              value={form.tipoCarga}
              onChange={handleChange}
              placeholder="Ej. Electrónica, Alimentos, Maquinaria"
              maxLength="100"
            />
            {errors.tipoCarga && (
              <span className="field-error" role="alert">{errors.tipoCarga}</span>
            )}
          </label>

          <div className="two-columns">
            <label>
              Tipo de vehículo requerido
              <select
                name="tipoVehiculoRequerido"
                value={form.tipoVehiculoRequerido}
                onChange={handleChange}
              >
                <option value="">Selecciona</option>
                <option value="Camioneta">Camioneta</option>
                <option value="Camión">Camión</option>
                <option value="Mula">Mula</option>
              </select>
              {errors.tipoVehiculoRequerido && (
                <span className="field-error" role="alert">{errors.tipoVehiculoRequerido}</span>
              )}
            </label>

            <label>
              Peso (toneladas)
              <input
                type="number"
                name="peso"
                value={form.peso}
                onChange={handleChange}
                min="0.01"
                step="0.01"
                placeholder="0.00"
              />
              {errors.peso && (
                <span className="field-error" role="alert">{errors.peso}</span>
              )}
            </label>
          </div>

          <label>
            Precio ofrecido (COP)
            <input
              type="number"
              name="precioOfrecido"
              value={form.precioOfrecido}
              onChange={handleChange}
              min="0.01"
              step="0.01"
              placeholder="0.00"
            />
            {errors.precioOfrecido && (
              <span className="field-error" role="alert">{errors.precioOfrecido}</span>
            )}
          </label>

          {/* ── Sección: Fechas ── */}
          <h2>Fechas</h2>

          <div className="two-columns">
            <label>
              Fecha de recogida
              <input
                type="datetime-local"
                name="fechaRecogida"
                value={form.fechaRecogida}
                onChange={handleChange}
              />
              {errors.fechaRecogida && (
                <span className="field-error" role="alert">{errors.fechaRecogida}</span>
              )}
            </label>

            <label>
              Fecha estimada de entrega
              <input
                type="datetime-local"
                name="fechaEntregaEstimada"
                value={form.fechaEntregaEstimada}
                onChange={handleChange}
              />
              {errors.fechaEntregaEstimada && (
                <span className="field-error" role="alert">{errors.fechaEntregaEstimada}</span>
              )}
            </label>
          </div>

          {/* ── Sección: Cita en puerto ── */}
          <h2>Cita en puerto</h2>

          <label className="role-option">
            <input
              type="checkbox"
              name="requiereCitaPuerto"
              checked={form.requiereCitaPuerto}
              onChange={handleChange}
            />
            Requiere cita en puerto
          </label>

          {form.requiereCitaPuerto && (
            <label>
              Número de cita
              <input
                name="numeroCita"
                value={form.numeroCita}
                onChange={handleChange}
                placeholder="Ej. SPRC-2024-001"
                maxLength="50"
              />
              {errors.numeroCita && (
                <span className="field-error" role="alert">{errors.numeroCita}</span>
              )}
            </label>
          )}

          {/* ── Enviar ── */}
          <button type="submit" disabled={cargando}>
            {cargando ? 'Publicando...' : 'Publicar solicitud'}
          </button>

          <RequestMessage mensaje={mensaje} error={errorGlobal} />

        </form>
      </div>
    </ProfileLayout>
  )
}

export default PublicarSolicitudPage
