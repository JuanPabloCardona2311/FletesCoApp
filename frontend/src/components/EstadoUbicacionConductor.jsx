import {
  MINUTOS_UBICACION_RECIENTE,
  debeCompartirUbicacion,
  inicioSeguimiento,
  minutosDesde,
  tiempoRelativo,
} from '../utils/geolocalizacion'

// Resume para el despachador qué tan reciente es la ubicación del conductor en un viaje activo.
function EstadoUbicacionConductor({ flete }) {
  if (!flete || !['ACEPTADA', 'EN_CURSO'].includes(flete.estado)) {
    return null
  }

  const enSeguimiento = debeCompartirUbicacion(flete)
  const tieneUbicacion = flete.conductorLat != null && flete.conductorLng != null
  const minutos = minutosDesde(flete.ubicacionActualizadaEn)

  if (!enSeguimiento) {
    const inicio = inicioSeguimiento(flete)
    return (
      <p className="ubicacion-estado ubicacion-espera">
        🕒 El seguimiento en vivo empieza el{' '}
        {inicio?.toLocaleString('es-CO', { dateStyle: 'medium', timeStyle: 'short' })} (24 h antes de la recogida).
        {tieneUbicacion && ` Ubicación al aceptar: ${tiempoRelativo(flete.ubicacionActualizadaEn)}.`}
      </p>
    )
  }

  if (!tieneUbicacion) {
    return (
      <p className="ubicacion-estado ubicacion-sin-senal">
        ⚠️ Aún no recibimos la ubicación del conductor.
      </p>
    )
  }

  if (minutos !== null && minutos >= MINUTOS_UBICACION_RECIENTE) {
    return (
      <p className="ubicacion-estado ubicacion-sin-senal">
        ⚠️ Sin señal reciente del conductor · última ubicación {tiempoRelativo(flete.ubicacionActualizadaEn)}.
        Puede estar en una zona sin cobertura o con la app cerrada.
      </p>
    )
  }

  return (
    <p className="ubicacion-estado ubicacion-en-vivo">
      <span className="ubicacion-punto" aria-hidden="true" />
      Ubicación en vivo · actualizada {tiempoRelativo(flete.ubicacionActualizadaEn)}
    </p>
  )
}

export default EstadoUbicacionConductor
