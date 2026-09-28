import { Navigate, Route, Routes } from 'react-router-dom'
import ConductorProfilePage from './pages/ConductorProfilePage'
import DespachadorProfilePage from './pages/DespachadorProfilePage'
import LoginPage from './pages/LoginPage'
import ProfileRedirect from './pages/ProfileRedirect'
import RegisterPage from './pages/RegisterPage'
import DetalleSolicitudPage from './pages/DetalleSolicitudPage'
import PublicarSolicitudPage from './pages/PublicarSolicitudPage'

function ProtectedRoute({ children, allowedRole }) {
  const token = localStorage.getItem('fleteco_token')

  if (!token) {
    return <Navigate to="/" replace />
  }

  if (allowedRole) {
    const tipoUsuario = localStorage.getItem('fleteco_tipo_usuario')
    if (tipoUsuario !== allowedRole) {
      return <Navigate to="/perfil" replace />
    }
  }

  return children
}

function App() {
  return (
    <Routes>
      <Route path="/" element={<LoginPage />} />
      <Route path="/registro" element={<RegisterPage />} />
      <Route
        path="/solicitudes/nueva"
        element={(
          <ProtectedRoute allowedRole="DESPACHADOR">
            <PublicarSolicitudPage />
          </ProtectedRoute>
        )}
      />
      <Route
        path="/solicitudes/:id"
        element={(
          <ProtectedRoute>
            <DetalleSolicitudPage />
          </ProtectedRoute>
        )}
      />
      <Route
        path="/perfil"
        element={(
          <ProtectedRoute>
            <ProfileRedirect />
          </ProtectedRoute>
        )}
      />
      <Route
        path="/perfil/conductor"
        element={(
          <ProtectedRoute>
            <ConductorProfilePage />
          </ProtectedRoute>
        )}
      />
      <Route
        path="/perfil/despachador"
        element={(
          <ProtectedRoute>
            <DespachadorProfilePage />
          </ProtectedRoute>
        )}
      />
    </Routes>
  )
}

export default App
