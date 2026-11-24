import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import ProfileLayout from '../components/ProfileLayout'
import { IconoVolver } from '../components/Iconos'
import RequestMessage from '../components/RequestMessage'
import IndicadorEnVivo from '../components/IndicadorEnVivo'
import adminClient from '../api/adminClient'
import { crearCanal } from '../utils/tiempoReal'

const BASE_ADMIN = import.meta.env.VITE_ADMIN_URL ?? 'http://localhost:8084'
const MAXIMO_ACTIVIDAD = 10
const ESPERA_RECARGA_MS = 500

// Texto en español de cada evento de solicitud que llega por el WebSocket.
function describirEvento(evento) {
  const id = evento.solicitudId
  switch (evento.tipo) {
    case 'SOLICITUD_CREADA':
      return `Solicitud #${id} publicada`
    case 'SOLICITUD_ACEPTADA':
      return `#${id} aceptada por un conductor`
    case 'VIAJE_INICIADO':
      return `#${id} en curso`
    case 'ENTREGA_MARCADA':
      return `#${id} entregada`
    case 'PAGO_LIBERADO':
      return `Pago de #${id} liberado`
    default:
      return null
  }
}

function AdminReportesPage() {
  const [resumen, setResumen] = useState(null)
  const [cargando, setCargando] = useState(true)
  const [descargando, setDescargando] = useState(false)
  const [error, setError] = useState('')
  const [estadoCanal, setEstadoCanal] = useState('reconectando')
  const [enlaceCaido, setEnlaceCaido] = useState(false)
  const [actividad, setActividad] = useState([])

  // Los números siempre salen de la base de datos; los eventos solo disparan la recarga.
  const cargarResumen = useCallback(async () => {
    try {
      const response = await adminClient.get('/api/admin/reportes/resumen')
      setResumen(response.data)
    } catch (requestError) {
      if (requestError.response?.status === 401) {
        // Sesión vencida: el interceptor de la API ya la cerró y lleva a /login.
      } else {
        setError(requestError.response?.data?.error || 'No fue posible cargar el resumen.')
      }
    } finally {
      setCargando(false)
    }
  }, [])

  useEffect(() => {
    cargarResumen()
  }, [cargarResumen])

  useEffect(() => {
    const canal = crearCanal(BASE_ADMIN, '/api/admin/ws')
    let temporizador = null
    // Si llegan varios eventos seguidos, se recarga una sola vez.
    const recargar = () => {
      clearTimeout(temporizador)
      temporizador = setTimeout(cargarResumen, ESPERA_RECARGA_MS)
    }

    const dejarDeOirEstado = canal.alCambiarEstado(setEstadoCanal)
    const dejarDeEscuchar = canal.escuchar((mensaje) => {
      if (mensaje.tipo === 'ENLACE_ESTADO') {
        setEnlaceCaido(!mensaje.conectado)
        return
      }
      if (mensaje.tipo === 'RESINCRONIZAR') {
        recargar()
        return
      }
      const texto = describirEvento(mensaje)
      if (!texto) return
      const entrada = {
        clave: `${mensaje.tipo}-${mensaje.solicitudId}-${mensaje.fecha}`,
        texto,
        hora: new Date(mensaje.fecha).toLocaleTimeString('es-CO'),
      }
      setActividad((anteriores) => [entrada, ...anteriores].slice(0, MAXIMO_ACTIVIDAD))
      recargar()
    })

    return () => {
      clearTimeout(temporizador)
      dejarDeEscuchar()
      dejarDeOirEstado()
    }
  }, [cargarResumen])

  const descargarCsv = async () => {
    setDescargando(true)
    setError('')
    try {
      const response = await adminClient.get('/api/admin/reportes/solicitudes/exportar', { responseType: 'blob' })
      const url = URL.createObjectURL(response.data)
      const enlace = document.createElement('a')
      enlace.href = url
      enlace.download = 'reporte-solicitudes.csv'
      enlace.click()
      URL.revokeObjectURL(url)
    } catch (requestError) {
      if (requestError.response?.status === 401) {
        // Sesión vencida: el interceptor de la API ya la cerró y lleva a /login.
      } else {
        setError('No fue posible descargar el reporte.')
      }
    } finally {
      setDescargando(false)
    }
  }

  const renderMap = (titulo, valores) => (
    <section className="summary-panel">
      <h2>{titulo}</h2>
      <div className="metric-list">
        {Object.entries(valores || {}).map(([clave, valor]) => (
          <div key={clave}><span>{clave}</span><strong>{valor}</strong></div>
        ))}
      </div>
    </section>
  )

  const enVivo = estadoCanal === 'conectado'

  return (
    <ProfileLayout role="Panel de Administrador" title="Reportes" subtitle="Consulta indicadores consolidados de la plataforma.">
      <div className="reportes-barra">
        <Link className="boton-volver" to="/perfil/admin"><IconoVolver />Volver al panel</Link>
        {/* Amarillo "Sin eventos" cuando no hay enlace con ms-solicitudes. */}
        <IndicadorEnVivo estado={estadoCanal} sinEventos={enlaceCaido} />
      </div>
      {avisoEnlaceVisible(enVivo, enlaceCaido) && (
        <p className="viaje-aviso reportes-aviso" role="status">
          No hay conexión con el servicio de solicitudes. Los datos se siguen leyendo de la base de datos y se actualizarán cuando vuelva.
        </p>
      )}
      <RequestMessage error={error} />
      {cargando ? <p>Cargando reportes...</p> : resumen && (
        <>
          <div className="profile-grid">
            {renderMap('Solicitudes por estado', resumen.solicitudesPorEstado)}
            {renderMap('Disputas por estado', resumen.disputasPorEstado)}
            {renderMap('Usuarios por estado', resumen.usuariosPorEstado)}
            {renderMap('Usuarios por tipo', resumen.usuariosPorTipo)}
          </div>
          <section className="profile-panel" style={{ marginTop: '24px' }}>
            <h2>Total de solicitudes: {resumen.totalSolicitudes}</h2>
            <button type="button" onClick={descargarCsv} disabled={descargando}>
              {descargando ? 'Descargando...' : 'Descargar CSV'}
            </button>
          </section>
        </>
      )}
      <section className="profile-panel" style={{ marginTop: '24px' }}>
        <h2>Actividad en vivo</h2>
        {actividad.length === 0 ? (
          <p className="solicitudes-vacio">Todavía no hay actividad desde que abriste esta página.</p>
        ) : (
          <ul className="actividad-lista">
            {actividad.map((entrada) => (
              <li key={entrada.clave}>
                <span>{entrada.texto}</span>
                <time>{entrada.hora}</time>
              </li>
            ))}
          </ul>
        )}
      </section>
    </ProfileLayout>
  )
}

// El aviso del enlace solo tiene sentido si el navegador está conectado al panel:
// si es el navegador el que no tiene conexión, ya lo dice el indicador.
function avisoEnlaceVisible(enVivo, enlaceCaido) {
  return enVivo && enlaceCaido
}

export default AdminReportesPage
