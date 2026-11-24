import { useEffect } from 'react'
import L from 'leaflet'
import { MapContainer, TileLayer, Marker, Popup, useMap, useMapEvents } from 'react-leaflet'
import 'leaflet/dist/leaflet.css'
import { IconoBandera, IconoUbicacion } from './Iconos'
import './SelectorRutaMap.css'

const ZOOM_PUNTO = 16

// Pines circulares arrastrables (Marker con divIcon de Leaflet; CircleMarker no se puede arrastrar).
const PIN_ORIGEN = L.divIcon({
  className: 'selector-ruta-map__pin selector-ruta-map__pin--origen',
  iconSize: [20, 20],
  iconAnchor: [10, 10],
  popupAnchor: [0, -10],
})
const PIN_DESTINO = L.divIcon({
  className: 'selector-ruta-map__pin selector-ruta-map__pin--destino',
  iconSize: [20, 20],
  iconAnchor: [10, 10],
  popupAnchor: [0, -10],
})

// Captura clics en el mapa y delega al padre según el modo activo.
// e.latlng.wrap() normaliza la longitud al rango [-180, 180].
function ClickHandler({ modo, onSeleccionar }) {
  useMapEvents({
    click(e) {
      const { lat, lng } = e.latlng.wrap()
      onSeleccionar(modo, lat, lng)
    },
  })
  return null
}

// Lleva la vista a los puntos cuando quedan fuera de ella (por ejemplo, al elegir una
// dirección escrita): con los dos puntos encuadra ambos; con uno, centra y acerca. Si el
// punto ya se ve (clic o arrastre en el mapa), no mueve la vista.
function AjustarVista({ origen, destino }) {
  const map = useMap()
  const origenLat = origen?.lat
  const origenLng = origen?.lng
  const destinoLat = destino?.lat
  const destinoLng = destino?.lng

  useEffect(() => {
    const puntos = [[origenLat, origenLng], [destinoLat, destinoLng]]
      .filter(([lat, lng]) => lat != null && lng != null)
    if (puntos.length === 0) return
    const visible = map.getBounds()
    const todosVisibles = puntos.every((punto) => visible.contains(punto))
    if (puntos.length === 2) {
      if (!todosVisibles) map.fitBounds(puntos, { padding: [40, 40], maxZoom: ZOOM_PUNTO })
    } else if (!todosVisibles || map.getZoom() < 12) {
      map.setView(puntos[0], ZOOM_PUNTO)
    }
  }, [map, origenLat, origenLng, destinoLat, destinoLng])

  return null
}

/**
 * SelectorRutaMap
 *
 * Props:
 *   origen        – { lat, lng } | null
 *   destino       – { lat, lng } | null
 *   modo          – 'ORIGEN' | 'DESTINO'
 *   onModoChange(modo)
 *   onSeleccionar(tipo, lat, lng)   tipo es 'ORIGEN' o 'DESTINO'
 *   onLimpiar(tipo)
 */
function SelectorRutaMap({ origen, destino, modo, onModoChange, onSeleccionar, onLimpiar }) {
  // Cuando el usuario marca un origen, pasar automáticamente al modo DESTINO
  useEffect(() => {
    if (modo === 'ORIGEN' && origen) {
      onModoChange('DESTINO')
    }
  }, [origen]) // eslint-disable-line react-hooks/exhaustive-deps

  // Al soltar un pin se usa la misma lógica que el clic en el mapa.
  const alSoltar = (tipo) => ({
    dragend(e) {
      const { lat, lng } = e.target.getLatLng().wrap()
      onSeleccionar(tipo, lat, lng)
    },
  })

  return (
    <div className="selector-ruta-map">
      {/* Controles de modo */}
      <div className="selector-ruta-map__controles">
        <button
          type="button"
          className={`selector-ruta-map__btn-modo${modo === 'ORIGEN' ? ' selector-ruta-map__btn-modo--activo' : ''}`}
          onClick={() => onModoChange('ORIGEN')}
        >
          <IconoUbicacion />Fijar origen
        </button>
        <button
          type="button"
          className={`selector-ruta-map__btn-modo${modo === 'DESTINO' ? ' selector-ruta-map__btn-modo--activo' : ''}`}
          onClick={() => onModoChange('DESTINO')}
        >
          <IconoBandera />Fijar destino
        </button>
        <span className="selector-ruta-map__indicador">
          Clic en el mapa para marcar: <strong>{modo === 'ORIGEN' ? 'Origen' : 'Destino'}</strong>
        </span>
      </div>

      {/* Mapa */}
      <MapContainer
        center={[4.5709, -74.2973]}
        zoom={5}
        className="selector-ruta-map__canvas"
      >
        <TileLayer
          attribution="&copy; OpenStreetMap contributors"
          url="https://tile.openstreetmap.org/{z}/{x}/{y}.png"
        />

        <ClickHandler modo={modo} onSeleccionar={onSeleccionar} />
        <AjustarVista origen={origen} destino={destino} />

        {origen && (
          <Marker
            position={[origen.lat, origen.lng]}
            icon={PIN_ORIGEN}
            draggable
            title="Origen (arrástralo para corregir)"
            eventHandlers={alSoltar('ORIGEN')}
          >
            <Popup>Origen</Popup>
          </Marker>
        )}

        {destino && (
          <Marker
            position={[destino.lat, destino.lng]}
            icon={PIN_DESTINO}
            draggable
            title="Destino (arrástralo para corregir)"
            eventHandlers={alSoltar('DESTINO')}
          >
            <Popup>Destino</Popup>
          </Marker>
        )}
      </MapContainer>

      {/* Chips de puntos marcados */}
      <div className="selector-ruta-map__puntos">
        <div className="selector-ruta-map__punto">
          <span className="selector-ruta-map__punto-dot selector-ruta-map__punto-dot--origen" />
          <span>Origen: {origen ? `${origen.lat.toFixed(5)}, ${origen.lng.toFixed(5)}` : 'Sin marcar'}</span>
          {origen && (
            <button
              type="button"
              className="selector-ruta-map__btn-limpiar"
              aria-label="Limpiar origen"
              onClick={() => onLimpiar('ORIGEN')}
            >
              ✕
            </button>
          )}
        </div>
        <div className="selector-ruta-map__punto">
          <span className="selector-ruta-map__punto-dot selector-ruta-map__punto-dot--destino" />
          <span>Destino: {destino ? `${destino.lat.toFixed(5)}, ${destino.lng.toFixed(5)}` : 'Sin marcar'}</span>
          {destino && (
            <button
              type="button"
              className="selector-ruta-map__btn-limpiar"
              aria-label="Limpiar destino"
              onClick={() => onLimpiar('DESTINO')}
            >
              ✕
            </button>
          )}
        </div>
      </div>
    </div>
  )
}

export default SelectorRutaMap
