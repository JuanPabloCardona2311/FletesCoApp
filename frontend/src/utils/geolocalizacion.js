// Geolocalización del conductor: permiso del navegador, errores legibles y reglas de seguimiento.

export const INTERVALO_ENVIO_MS = 20000
export const HORAS_ANTES_RECOGIDA = 24
export const MINUTOS_UBICACION_RECIENTE = 2
export const EVENTO_VIAJE_ACTUALIZADO = 'fleteco:viaje-actualizado'

export const OPCIONES_GPS = {
  enableHighAccuracy: true,
  timeout: 15000,
  maximumAge: 10000,
}

const INSTRUCCIONES_REACTIVAR =
  'Para activarlo, haz clic en el candado 🔒 junto a la dirección de la página, busca "Ubicación" y elige "Permitir". Luego vuelve a intentarlo.'

const crearError = (codigo, mensaje) => ({ codigo, mensaje })

export function traducirErrorGeolocalizacion(error) {
  switch (error?.code) {
    case 1:
      return crearError('DENEGADO', `No diste permiso para usar tu ubicación. ${INSTRUCCIONES_REACTIVAR}`)
    case 2:
      return crearError('NO_DISPONIBLE', 'No pudimos detectar tu ubicación. Revisa que el GPS o los servicios de ubicación de tu dispositivo estén activados.')
    case 3:
      return crearError('TIEMPO_AGOTADO', 'Obtener tu ubicación está tardando demasiado. Revisa tu señal de GPS e inténtalo de nuevo.')
    default:
      return crearError('DESCONOCIDO', 'Ocurrió un error inesperado al obtener tu ubicación. Inténtalo de nuevo.')
  }
}

// Detecta los casos en que el navegador ni siquiera puede mostrar el mensaje de permiso.
export function verificarSoporteGeolocalizacion() {
  if (!window.isSecureContext) {
    return crearError('INSEGURO', 'Tu navegador solo permite compartir la ubicación en conexiones seguras (HTTPS). Abre FletesCo desde una dirección https:// o desde localhost.')
  }
  if (!('geolocation' in navigator)) {
    return crearError('NO_SOPORTADO', 'Tu navegador no permite compartir la ubicación. Usa una versión actualizada de Chrome, Edge, Firefox o Safari.')
  }
  return null
}

async function consultarEstadoPermiso() {
  try {
    const estado = await navigator.permissions?.query({ name: 'geolocation' })
    return estado?.state || 'prompt'
  } catch {
    return 'prompt'
  }
}

// Pide el permiso (si hace falta) y devuelve { latitud, longitud }. Si falla, lanza { codigo, mensaje }.
export async function obtenerUbicacionActual() {
  const errorSoporte = verificarSoporteGeolocalizacion()
  if (errorSoporte) throw errorSoporte

  // Si el permiso ya fue bloqueado, el navegador no vuelve a preguntar: explicamos cómo reactivarlo.
  if (await consultarEstadoPermiso() === 'denied') {
    throw crearError('DENEGADO', `Tienes bloqueado el acceso a tu ubicación para FletesCo. ${INSTRUCCIONES_REACTIVAR}`)
  }

  return new Promise((resolve, reject) => {
    navigator.geolocation.getCurrentPosition(
      (posicion) => resolve({
        latitud: Number(posicion.coords.latitude.toFixed(7)),
        longitud: Number(posicion.coords.longitude.toFixed(7)),
      }),
      (error) => reject(traducirErrorGeolocalizacion(error)),
      OPCIONES_GPS,
    )
  })
}

// Momento desde el que un viaje ACEPTADA empieza a compartir ubicación (24 h antes de la recogida).
export function inicioSeguimiento(viaje) {
  if (!viaje?.fechaRecogida) return null
  const recogida = new Date(viaje.fechaRecogida)
  return new Date(recogida.getTime() - HORAS_ANTES_RECOGIDA * 60 * 60 * 1000)
}

export function debeCompartirUbicacion(viaje, ahora = new Date()) {
  if (!viaje) return false
  if (viaje.estado === 'EN_CURSO') return true
  if (viaje.estado !== 'ACEPTADA') return false
  const inicio = inicioSeguimiento(viaje)
  return !inicio || ahora >= inicio
}

export function minutosDesde(fechaStr, ahora = new Date()) {
  if (!fechaStr) return null
  return Math.max(0, Math.floor((ahora - new Date(fechaStr)) / 60000))
}

export function tiempoRelativo(fechaStr) {
  const minutos = minutosDesde(fechaStr)
  if (minutos === null) return 'sin datos'
  if (minutos < 1) return 'hace unos segundos'
  if (minutos < 60) return `hace ${minutos} min`
  const horas = Math.floor(minutos / 60)
  if (horas < 24) return `hace ${horas} h`
  return `hace ${Math.floor(horas / 24)} d`
}

export function avisarViajeActualizado() {
  window.dispatchEvent(new Event(EVENTO_VIAJE_ACTUALIZADO))
}
