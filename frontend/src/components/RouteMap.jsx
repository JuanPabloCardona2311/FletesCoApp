import { useEffect, useState } from 'react'

import {
    MapContainer,
    TileLayer,
    CircleMarker,
    Popup,
    Polyline,
    useMap,
} from 'react-leaflet'

import { obtenerRutaSugerida } from '../api/routeService'

import 'leaflet/dist/leaflet.css'
import './RouteMap.css'

function AjustarVista({
    origenLat,
    origenLng,
    destinoLat,
    destinoLng,
}) {
    const map = useMap()

    useEffect(() => {
        map.fitBounds([
            [origenLat, origenLng],
            [destinoLat, destinoLng],
        ])
    }, [
        map,
        origenLat,
        origenLng,
        destinoLat,
        destinoLng,
    ])

    return null
}

function formatearDuracion(minutosTotales) {
    const totalRedondeado = Math.round(minutosTotales)

    const horas = Math.floor(totalRedondeado / 60)
    const minutos = totalRedondeado % 60

    if (horas === 0) {
        return `${minutos} min`
    }

    return `${horas} h ${minutos} min`
}

function RouteMap({
    origenLat,
    origenLng,
    destinoLat,
    destinoLng,
    ubicacionConductor = null,
}) {
    const [ruta, setRuta] = useState([])
    const [cargando, setCargando] = useState(false)
    const [error, setError] = useState('')
    const [infoRuta, setInfoRuta] = useState(null)

    useEffect(() => {
        async function cargarRuta() {
            setCargando(true)
            setError('')

            try {
                const resultado = await obtenerRutaSugerida(
                    origenLat,
                    origenLng,
                    destinoLat,
                    destinoLng,
                )

                setRuta(resultado.puntos)
                setInfoRuta({
                    distanciaKm: resultado.distanciaKm,
                    duracionMinutos: resultado.duracionMinutos,
                })
            } catch (err) {
                setError(err.message)
                setRuta([])
                setInfoRuta(null)
            } finally {
                setCargando(false)
            }
        }

        cargarRuta()
    }, [
        origenLat,
        origenLng,
        destinoLat,
        destinoLng,
    ])

    return (
        <>
            <MapContainer
                center={[4.5709, -74.2973]}
                zoom={5}
                className="route-map"
            >
                <TileLayer
                    attribution='&copy; OpenStreetMap contributors'
                    url="https://tile.openstreetmap.org/{z}/{x}/{y}.png"
                />

                <AjustarVista
                    origenLat={origenLat}
                    origenLng={origenLng}
                    destinoLat={destinoLat}
                    destinoLng={destinoLng}
                />

                {ruta.length > 0 && (
                    <Polyline positions={ruta} pathOptions={{ color: '#315f8c', weight: 4, opacity: 0.85 }} />
                )}

                <CircleMarker
                    center={[origenLat, origenLng]}
                    radius={9}
                    pathOptions={{ color: '#ffffff', weight: 3, fillColor: '#2b5b3e', fillOpacity: 1 }}
                >
                    <Popup>Origen</Popup>
                </CircleMarker>

                <CircleMarker
                    center={[destinoLat, destinoLng]}
                    radius={9}
                    pathOptions={{ color: '#ffffff', weight: 3, fillColor: '#b3261e', fillOpacity: 1 }}
                >
                    <Popup>Destino</Popup>
                </CircleMarker>

                {ubicacionConductor && (
                    <CircleMarker
                        center={[ubicacionConductor.lat, ubicacionConductor.lng]}
                        radius={11}
                        pathOptions={{ color: '#ffffff', weight: 3, fillColor: '#d9a441', fillOpacity: 1 }}
                    >
                        <Popup>🚚 Conductor</Popup>
                    </CircleMarker>
                )}
            </MapContainer>

            <div className="route-map-leyenda">
                <span><i className="route-map-punto route-map-punto-origen" /> Origen</span>
                <span><i className="route-map-punto route-map-punto-destino" /> Destino</span>
                {ubicacionConductor && (
                    <span><i className="route-map-punto route-map-punto-conductor" /> Conductor</span>
                )}
            </div>

            {infoRuta && (
                <div className="route-map-stats">
                    <div>
                        <span>Distancia aproximada</span>
                        <strong>{infoRuta.distanciaKm.toFixed(1)} km</strong>
                    </div>
                    <div>
                        <span>Duración estimada</span>
                        <strong>{formatearDuracion(infoRuta.duracionMinutos)}</strong>
                    </div>
                </div>
            )}

            {cargando && (
                <p className="route-map-mensaje">Calculando ruta sugerida...</p>
            )}

            {error && (
                <p className="route-map-mensaje route-map-error">{error}</p>
            )}
        </>
    )
}

export default RouteMap