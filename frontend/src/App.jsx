import { Navigate, Route, Routes } from 'react-router-dom'
import ConductorProfilePage from './pages/ConductorProfilePage'
import SolicitudesDisponiblesPage from './pages/SolicitudesDisponiblesPage'
import DespachadorProfilePage from './pages/DespachadorProfilePage'
import AdminProfilePage from './pages/AdminProfilePage'
import LandingPage from './pages/LandingPage'
import LoginPage from './pages/LoginPage'
import ProfileRedirect from './pages/ProfileRedirect'
import RegisterPage from './pages/RegisterPage'
import DetalleSolicitudPage from './pages/DetalleSolicitudPage'
import PublicarSolicitudPage from './pages/PublicarSolicitudPage'
import AdminUsuariosPage from './pages/AdminUsuariosPage'
import AdminDisputasPage from './pages/AdminDisputasPage'
import AdminReportesPage from './pages/AdminReportesPage'
import SeguimientoUbicacion from './components/SeguimientoUbicacion'

function ProtectedRoute({ children, allowedRole }) {
  const token = localStorage.getItem('fleteco_token')
  const userRole = localStorage.getItem('fleteco_tipo_usuario')

  if (!token) {
    return <Navigate to="/login" replace />
  }

  if (allowedRole && userRole !== allowedRole) {
    return <Navigate to="/perfil" replace />
  }

  return children
}

function App() {
  return (
    <>
    <SeguimientoUbicacion />
    <Routes>
      <Route path="/" element={<LandingPage />} />
      <Route path="/login" element={<LoginPage />} />
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
        path="/solicitudes/disponibles"
        element={(
          <ProtectedRoute allowedRole="CONDUCTOR">
            <SolicitudesDisponiblesPage />
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
          <ProtectedRoute allowedRole="CONDUCTOR">
            <ConductorProfilePage />
          </ProtectedRoute>
        )}
      />
      <Route
        path="/perfil/despachador"
        element={(
          <ProtectedRoute allowedRole="DESPACHADOR">
            <DespachadorProfilePage />
          </ProtectedRoute>
        )}
      />
      <Route
        path="/perfil/admin"
        element={(
          <ProtectedRoute allowedRole="ADMINISTRADOR">
            <AdminProfilePage />
          </ProtectedRoute>
        )}
      />
      <Route
        path="/admin/usuarios"
        element={(
          <ProtectedRoute allowedRole="ADMINISTRADOR">
            <AdminUsuariosPage />
          </ProtectedRoute>
        )}
      />
      <Route
        path="/admin/disputas"
        element={(
          <ProtectedRoute allowedRole="ADMINISTRADOR">
            <AdminDisputasPage />
          </ProtectedRoute>
        )}
      />
      <Route
        path="/admin/reportes"
        element={(
          <ProtectedRoute allowedRole="ADMINISTRADOR">
            <AdminReportesPage />
          </ProtectedRoute>
        )}
      />
    </Routes>
    </>
  )
}

export default App
