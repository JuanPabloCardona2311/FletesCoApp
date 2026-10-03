import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import ProfileLayout from '../components/ProfileLayout'
import RequestMessage from '../components/RequestMessage'
import adminClient from '../api/adminClient'

function AdminReportesPage() {
  const navigate = useNavigate()
  const [resumen, setResumen] = useState(null)
  const [cargando, setCargando] = useState(true)
  const [descargando, setDescargando] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    const cargarResumen = async () => {
      try {
        const response = await adminClient.get('/api/admin/reportes/resumen')
        setResumen(response.data)
      } catch (requestError) {
        if (requestError.response?.status === 401) {
          localStorage.removeItem('fleteco_token')
          localStorage.removeItem('fleteco_tipo_usuario')
          navigate('/')
        } else {
          setError(requestError.response?.data?.error || 'No fue posible cargar el resumen.')
        }
      } finally {
        setCargando(false)
      }
    }
    cargarResumen()
  }, [navigate])

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
        localStorage.removeItem('fleteco_token')
        localStorage.removeItem('fleteco_tipo_usuario')
        navigate('/')
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

  return (
    <ProfileLayout role="Panel de Administrador" title="Reportes" subtitle="Consulta indicadores consolidados de la plataforma.">
      <p><Link to="/perfil/admin">Volver al panel</Link></p>
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
    </ProfileLayout>
  )
}

export default AdminReportesPage
