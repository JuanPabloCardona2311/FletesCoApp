import { useCallback, useEffect, useRef, useState } from 'react'
import { useLocation } from 'react-router-dom'
import perfilClient from '../api/perfilClient'
import solicitudesClient from '../api/solicitudesClient'
import {
  EVENTO_VIAJE_ACTUALIZADO,
  INTERVALO_ENVIO_MS,
  OPCIONES_GPS,
  debeCompartirUbicacion,
  inicioSeguimiento,
  traducirErrorGeolocalizacion,
  verificarSoporteGeolocalizacion,
} from '../utils/geolocalizacion'
import { useAvisosSolicitudes } from '../utils/tiempoReal'
import { useSesion } from '../context/useSesion'
import { IconoAlerta, IconoReloj } from './Iconos'
import './SeguimientoUbicacion.css'

const INTERVALO_REVISAR_VIAJE_MS = 60000
const AVISOS_DEL_VIAJE = ['SOLICITUD_ACEPTADA', 'VIAJE_INICIADO', 'ENTREGA_MARCADA', 'PAGO_LIBERADO']

// Envía la ubicación del conductor mientras tiene un viaje activo, en cualquier página de la app.
function SeguimientoUbicacion() {
  const location = useLocation()
  const { usuario } = useSesion()
  const esConductor = usuario?.tipoUsuario === 'CONDUCTOR'

  const [viaje, setViaje] = useState(null)
  const [errorGps, setErrorGps] = useState('')
  const [errorEnvio, setErrorEnvio] = useState('')
  const [compartiendo, setCompartiendo] = useState(false)
  const ultimaPosicion = useRef(null)
  const yaEnvio = useRef(false)
  const montado = useRef(true)

  useEffect(() => {
    montado.current = true
    return () => {
      montado.current = false
    }
  }, [])

  const cargarViaje = useCallback(async () => {
    try {
      const response = await solicitudesClient.get('/api/solicitudes/aceptada')
      if (montado.current) setViaje(response.data)
    } catch {
      if (montado.current) setViaje(null)
    }
  }, [])

  useEffect(() => {
    if (!esConductor) {
      setViaje(null)
      return undefined
    }

    cargarViaje()
    window.addEventListener(EVENTO_VIAJE_ACTUALIZADO, cargarViaje)
    return () => window.removeEventListener(EVENTO_VIAJE_ACTUALIZADO, cargarViaje)
  }, [esConductor, location.pathname, cargarViaje])

  // Avisos del viaje del conductor por WebSocket. El respaldo se queda en 60 s, como la
  // revisión que había antes, tanto conectado como reconectando.
  useAvisosSolicitudes({
    activo: esConductor,
    recargar: cargarViaje,
    respaldoSinConexionMs: INTERVALO_REVISAR_VIAJE_MS,
    respaldoConectadoMs: INTERVALO_REVISAR_VIAJE_MS,
    alEvento: (aviso) => {
      if (AVISOS_DEL_VIAJE.includes(aviso.tipo)) cargarViaje()
    },
  })

  const compartir = debeCompartirUbicacion(viaje)

  useEffect(() => {
    if (!compartir) {
      setCompartiendo(false)
      setErrorGps('')
      setErrorEnvio('')
      return undefined
    }

    const errorSoporte = verificarSoporteGeolocalizacion()
    if (errorSoporte) {
      setErrorGps(errorSoporte.mensaje)
      return undefined
    }

    yaEnvio.current = false

    const enviarUbicacion = async () => {
      if (!ultimaPosicion.current) return
      try {
        await perfilClient.put('/api/perfiles/conductor/ubicacion', ultimaPosicion.current)
        yaEnvio.current = true
        setErrorEnvio('')
      } catch {
        setErrorEnvio('No pudimos enviar tu ubicación al despachador. Revisa tu conexión a internet.')
      }
    }

    const idSeguimiento = navigator.geolocation.watchPosition(
      (posicion) => {
        ultimaPosicion.current = {
          latitud: Number(posicion.coords.latitude.toFixed(7)),
          longitud: Number(posicion.coords.longitude.toFixed(7)),
        }
        setErrorGps('')
        setCompartiendo(true)
        // La primera posición se envía de inmediato; las siguientes cada INTERVALO_ENVIO_MS.
        if (!yaEnvio.current) enviarUbicacion()
      },
      (error) => {
        setCompartiendo(false)
        setErrorGps(traducirErrorGeolocalizacion(error).mensaje)
      },
      OPCIONES_GPS,
    )
    const intervaloEnvio = setInterval(enviarUbicacion, INTERVALO_ENVIO_MS)

    return () => {
      navigator.geolocation.clearWatch(idSeguimiento)
      clearInterval(intervaloEnvio)
    }
  }, [compartir])

  if (!esConductor || !viaje || !['ACEPTADA', 'EN_CURSO'].includes(viaje.estado)) {
    return null
  }

  if (!compartir) {
    const inicio = inicioSeguimiento(viaje)
    return (
      <div className="seguimiento-aviso seguimiento-espera" role="status">
        <IconoReloj />Compartirás tu ubicación con el despachador desde el{' '}
        {inicio?.toLocaleString('es-CO', { dateStyle: 'medium', timeStyle: 'short' })} (24 h antes de la recogida).
      </div>
    )
  }

  const mensajeError = errorGps || errorEnvio
  if (mensajeError) {
    return (
      <div className="seguimiento-aviso seguimiento-error" role="alert">
        <strong><IconoAlerta />El despachador no está recibiendo tu ubicación</strong>
        <span>{mensajeError}</span>
      </div>
    )
  }

  return (
    <div className="seguimiento-aviso seguimiento-activo" role="status">
      <span className="seguimiento-pulso" aria-hidden="true" />
      {compartiendo
        ? `Compartiendo tu ubicación · Solicitud #${viaje.id}`
        : 'Obteniendo tu ubicación...'}
    </div>
  )
}

export default SeguimientoUbicacion
