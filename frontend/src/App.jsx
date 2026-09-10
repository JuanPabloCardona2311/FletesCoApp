import { Navigate, Route, Routes } from 'react-router-dom'
import ConductorProfilePage from './pages/ConductorProfilePage'
import DespachadorProfilePage from './pages/DespachadorProfilePage'
import LoginPage from './pages/LoginPage'
import ProfileRedirect from './pages/ProfileRedirect'
import RegisterPage from './pages/RegisterPage'
import DetalleSolicitudPage from './pages/DetalleSolicitudPage'

function ProtectedRoute({ children }) {
  const token = localStorage.getItem('fleteco_token')

  if (!token) {
    return <Navigate to="/" replace />
  }

  return children
}

function App() {
  return (
    <Routes>
      <Route path="/" element={<LoginPage />} />
      <Route path="/registro" element={<RegisterPage />} />
      <Route
        path="/solicitudes/:id"
        element={<DetalleSolicitudPage />}
      <Route path="/perfil" element={<ProfileRedirect />} />
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
