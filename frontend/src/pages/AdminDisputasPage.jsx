import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import ProfileLayout from '../components/ProfileLayout'
import RequestMessage from '../components/RequestMessage'
import adminClient from '../api/adminClient'

function AdminDisputasPage() {
  const navigate = useNavigate()
  const [disputas, setDisputas] = useState([])
  const [resoluciones, setResoluciones] = useState({})
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState('')
  const [mensaje, setMensaje] = useState('')

  const cargarDisputas = async () => {
    setCargando(true)
    try {
      const response = await adminClient.get('/api/admin/disputas')
      setDisputas(response.data)
    } catch (requestError) {
      if (requestError.response?.status === 401) {
        localStorage.removeItem('fleteco_token')
        localStorage.removeItem('fleteco_tipo_usuario')
        navigate('/')
      } else {
        setError(requestError.response?.data?.error || 'No fue posible cargar las disputas.')
      }
    } finally {
      setCargando(false)
    }
  }

  useEffect(() => { cargarDisputas() }, [])

  const resolver = async (disputa) => {
    setError('')
    setMensaje('')
    try {
      await adminClient.patch(`/api/admin/disputas/${disputa.id}/resolver`, {
        resolucion: resoluciones[disputa.id] || '',
      })
      setMensaje('Disputa resuelta correctamente.')
      await cargarDisputas()
    } catch (requestError) {
      if (requestError.response?.status === 401) {
        localStorage.removeItem('fleteco_token')
        localStorage.removeItem('fleteco_tipo_usuario')
        navigate('/')
      } else {
        setError(requestError.response?.data?.error || 'No fue posible resolver la disputa.')
      }
    }
  }

  return (
    <ProfileLayout role="Panel de Administrador" title="Gestión de disputas" subtitle="Revisa y resuelve incidencias reportadas.">
      <p><Link to="/perfil/admin">Volver al panel</Link></p>
      <RequestMessage mensaje={mensaje} error={error} />
      {cargando ? <p>Cargando disputas...</p> : (
        <div className="profile-grid">
          {disputas.map((disputa) => (
            <section className="profile-panel" key={disputa.id}>
              <div className="panel-heading"><h2>Disputa #{disputa.id}</h2><span className="status-chip">{disputa.estado}</span></div>
              <p><strong>Solicitud:</strong> {disputa.solicitudId}</p>
              <p><strong>Reporta:</strong> {disputa.usuarioReportaNombre}</p>
              <p><strong>Motivo:</strong> {disputa.motivo}</p>
              {disputa.resolucion && <p><strong>Resolución:</strong> {disputa.resolucion}</p>}
              {(disputa.estado === 'ABIERTA' || disputa.estado === 'EN_REVISION') && (
                <div>
                  <label>Resolución
                    <input value={resoluciones[disputa.id] || ''} onChange={(event) => setResoluciones({ ...resoluciones, [disputa.id]: event.target.value })} maxLength="500" />
                  </label>
                  <button type="button" onClick={() => resolver(disputa)}>Resolver</button>
                </div>
              )}
            </section>
          ))}
        </div>
      )}
    </ProfileLayout>
  )
}

export default AdminDisputasPage
