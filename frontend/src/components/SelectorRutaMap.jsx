import { useEffect } from 'react'
import { MapContainer, TileLayer, CircleMarker, Popup, useMapEvents } from 'react-leaflet'
import 'leaflet/dist/leaflet.css'
import './SelectorRutaMap.css'

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

  return (
    <div className="selector-ruta-map">
      {/* Controles de modo */}
      <div className="selector-ruta-map__controles">
        <button
          type="button"
          className={`selector-ruta-map__btn-modo${modo === 'ORIGEN' ? ' selector-ruta-map__btn-modo--activo' : ''}`}
          onClick={() => onModoChange('ORIGEN')}
        >
          📍 Fijar origen
        </button>
        <button
          type="button"
          className={`selector-ruta-map__btn-modo${modo === 'DESTINO' ? ' selector-ruta-map__btn-modo--activo' : ''}`}
          onClick={() => onModoChange('DESTINO')}
        >
          🏁 Fijar destino
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

        {origen && (
          <CircleMarker
            center={[origen.lat, origen.lng]}
            radius={9}
            pathOptions={{ color: '#2563eb', fillColor: '#3b82f6', fillOpacity: 0.9 }}
          >
            <Popup>Origen</Popup>
          </CircleMarker>
        )}

        {destino && (
          <CircleMarker
            center={[destino.lat, destino.lng]}
            radius={9}
            pathOptions={{ color: '#b91c1c', fillColor: '#ef4444', fillOpacity: 0.9 }}
          >
            <Popup>Destino</Popup>
          </CircleMarker>
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
