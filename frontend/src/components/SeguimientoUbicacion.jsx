import { useEffect, useRef, useState } from 'react'
import { useLocation } from 'react-router-dom'
import client from '../api/client'
import {
  EVENTO_VIAJE_ACTUALIZADO,
  INTERVALO_ENVIO_MS,
  OPCIONES_GPS,
  debeCompartirUbicacion,
  inicioSeguimiento,
  traducirErrorGeolocalizacion,
  verificarSoporteGeolocalizacion,
} from '../utils/geolocalizacion'
import './SeguimientoUbicacion.css'

const INTERVALO_REVISAR_VIAJE_MS = 60000

// Envía la ubicación del conductor mientras tiene un viaje activo, en cualquier página de la app.
function SeguimientoUbicacion() {
  const location = useLocation()
  const esConductor = Boolean(localStorage.getItem('fleteco_token'))
    && localStorage.getItem('fleteco_tipo_usuario') === 'CONDUCTOR'

  const [viaje, setViaje] = useState(null)
  const [errorGps, setErrorGps] = useState('')
  const [errorEnvio, setErrorEnvio] = useState('')
  const [compartiendo, setCompartiendo] = useState(false)
  const ultimaPosicion = useRef(null)
  const yaEnvio = useRef(false)

  useEffect(() => {
    if (!esConductor) {
      setViaje(null)
      return undefined
    }

    let cancelado = false
    const cargarViaje = async () => {
      try {
        const response = await client.get('/api/solicitudes/aceptada')
        if (!cancelado) setViaje(response.data)
      } catch {
        if (!cancelado) setViaje(null)
      }
    }

    cargarViaje()
    const intervalo = setInterval(cargarViaje, INTERVALO_REVISAR_VIAJE_MS)
    window.addEventListener(EVENTO_VIAJE_ACTUALIZADO, cargarViaje)

    return () => {
      cancelado = true
      clearInterval(intervalo)
      window.removeEventListener(EVENTO_VIAJE_ACTUALIZADO, cargarViaje)
    }
  }, [esConductor, location.pathname])

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
        await client.put('/api/perfiles/conductor/ubicacion', ultimaPosicion.current)
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
        🕒 Compartirás tu ubicación con el despachador desde el{' '}
        {inicio?.toLocaleString('es-CO', { dateStyle: 'medium', timeStyle: 'short' })} (24 h antes de la recogida).
      </div>
    )
  }

  const mensajeError = errorGps || errorEnvio
  if (mensajeError) {
    return (
      <div className="seguimiento-aviso seguimiento-error" role="alert">
        <strong>⚠️ El despachador no está recibiendo tu ubicación</strong>
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
