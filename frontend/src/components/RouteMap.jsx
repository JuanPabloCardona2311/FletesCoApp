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

                <CircleMarker
                    center={[origenLat, origenLng]}
                    radius={8}
                >
                    <Popup>Origen</Popup>
                </CircleMarker>

                <CircleMarker
                    center={[destinoLat, destinoLng]}
                    radius={8}
                >
                    <Popup>Destino</Popup>
                </CircleMarker>

                {ruta.length > 0 && (
                    <Polyline positions={ruta} />
                )}
            </MapContainer>

            {infoRuta && (
                <div>
                    <p>
                        <strong>Distancia aproximada:</strong>{' '}
                        {infoRuta.distanciaKm.toFixed(1)} km
                    </p>

                    <p>
                        <strong>Duración estimada:</strong>{' '}
                        {formatearDuracion(infoRuta.duracionMinutos)}
                    </p>
                </div>
            )}

            {cargando && (
                <p>Calculando ruta sugerida...</p>
            )}

            {error && (
                <p>{error}</p>
            )}
        </>
    )
}

export default RouteMap