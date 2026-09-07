import { Route, Routes } from 'react-router-dom'
import ConductorProfilePage from './pages/ConductorProfilePage'
import DespachadorProfilePage from './pages/DespachadorProfilePage'
import LoginPage from './pages/LoginPage'
import ProfileRedirect from './pages/ProfileRedirect'
import RegisterPage from './pages/RegisterPage'

function App() {
  return (
    <Routes>
      <Route path="/" element={<LoginPage />} />
      <Route path="/registro" element={<RegisterPage />} />
      <Route path="/perfil" element={<ProfileRedirect />} />
      <Route path="/perfil/conductor" element={<ConductorProfilePage />} />
      <Route path="/perfil/despachador" element={<DespachadorProfilePage />} />
    </Routes>
  )
}

export default App
