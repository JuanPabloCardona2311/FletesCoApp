const OSRM_URL =
  'https://router.project-osrm.org/route/v1/driving'

export async function obtenerRutaSugerida(
  origenLat,
  origenLng,
  destinoLat,
  destinoLng,
) {
  const coordenadas =
    `${origenLng},${origenLat};${destinoLng},${destinoLat}`

  const params = new URLSearchParams({
    overview: 'full',
    geometries: 'geojson',
    steps: 'false',
  })

  const response = await fetch(
    `${OSRM_URL}/${coordenadas}?${params}`,
  )

  if (!response.ok) {
    throw new Error(
      'No fue posible consultar la ruta.',
    )
  }

  const data = await response.json()

  if (data.code !== 'Ok' || !data.routes?.length) {
    throw new Error(
      'No se encontró una ruta entre el origen y el destino.',
    )
  }

  const ruta = data.routes[0]

  const puntos = ruta.geometry.coordinates.map(
    ([lng, lat]) => [lat, lng],
  )

  return {
    puntos,
    distanciaKm: ruta.distance / 1000,
    duracionMinutos: ruta.duration / 60,
  }
}