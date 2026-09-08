import { Route, Routes } from 'react-router-dom'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import DetalleSolicitudPage from './pages/DetalleSolicitudPage'

function App() {
  return (
    <Routes>
      <Route path="/" element={<LoginPage />} />
      <Route path="/registro" element={<RegisterPage />} />
      <Route
        path="/solicitudes/:id"
        element={<DetalleSolicitudPage />}
      />
    </Routes>
  )
}

export default App
