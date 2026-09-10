import { Navigate, Route, Routes } from 'react-router-dom'
import ConductorProfilePage from './pages/ConductorProfilePage'
import DespachadorProfilePage from './pages/DespachadorProfilePage'
import LoginPage from './pages/LoginPage'
import ProfileRedirect from './pages/ProfileRedirect'
import RegisterPage from './pages/RegisterPage'

function ProtectedRoute({ children, allowedRole }) {
  const token = localStorage.getItem('fleteco_token')
  const userRole = localStorage.getItem('fleteco_tipo_usuario')

  if (!token) {
    return <Navigate to="/" replace />
  }

  if (allowedRole && userRole !== allowedRole) {
    return <Navigate to="/perfil" replace />
  }

  return children
}

function App() {
  return (
    <Routes>
      <Route path="/" element={<LoginPage />} />
      <Route path="/registro" element={<RegisterPage />} />
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
    </Routes>
  )
}

export default App
