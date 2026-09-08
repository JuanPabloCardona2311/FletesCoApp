import RouteMap from '../components/RouteMap'

function RutaPage() {
  const origen = {
    lat: 4.711,
    lng: -74.0721,
  }

  const destino = {
    lat: 6.2442,
    lng: -75.5812,
  }

  return (
    <main style={{ padding: '2rem' }}>
      <h1>Ruta sugerida del flete</h1>

      <p>
        Bogotá → Medellín
      </p>

      <RouteMap
        origenLat={origen.lat}
        origenLng={origen.lng}
        destinoLat={destino.lat}
        destinoLng={destino.lng}
      />
    </main>
  )
}

export default RutaPage