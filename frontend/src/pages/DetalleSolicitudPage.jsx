import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'

import client from '../api/client'
import RouteMap from '../components/RouteMap'

function DetalleSolicitudPage() {
    const { id } = useParams()

    const [solicitud, setSolicitud] = useState(null)
    const [cargando, setCargando] = useState(true)
    const [error, setError] = useState('')
    useEffect(() => {
        async function cargarSolicitud() {
            setCargando(true)
            setError('')

            try {
                const response = await client.get(
                    `/api/solicitudes/${id}`,
                )

                setSolicitud(response.data)
            } catch (err) {
                if (err.response?.status === 403) {
                    setError(
                        'No tienes permiso para consultar esta solicitud.',
                    )
                } else if (err.response?.status === 404) {
                    setError('La solicitud no existe.')
                } else {
                    setError(
                        'No fue posible cargar la solicitud.',
                    )
                }

                setSolicitud(null)
            } finally {
                setCargando(false)
            }
        }

        cargarSolicitud()
    }, [id])
    if (cargando) {
        return <p>Cargando solicitud...</p>
    }

    if (error) {
        return <p>{error}</p>
    }

    if (!solicitud) {
        return null
    }
    return (
        <main style={{ padding: '2rem' }}>
            <h1>Detalle del flete</h1>

            <p>
                <strong>Origen:</strong>{' '}
                {solicitud.origen}
            </p>

            <p>
                <strong>Destino:</strong>{' '}
                {solicitud.destino}
            </p>

            <p>
                <strong>Tipo de carga:</strong>{' '}
                {solicitud.tipoCarga}
            </p>

            <p>
                <strong>Precio ofrecido:</strong>{' '}
                {solicitud.precioOfrecido}
            </p>

            <h2>Ruta sugerida</h2>

            <RouteMap
                origenLat={Number(solicitud.origenLat)}
                origenLng={Number(solicitud.origenLng)}
                destinoLat={Number(solicitud.destinoLat)}
                destinoLng={Number(solicitud.destinoLng)}
            />
        </main>
    )

}
export default DetalleSolicitudPage
