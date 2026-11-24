import { Navigate } from 'react-router-dom'
import { useSesion } from '../context/useSesion'

function ProfileRedirect() {
  const { usuario } = useSesion()
  const tipoUsuario = usuario?.tipoUsuario

  if (tipoUsuario === 'DESPACHADOR') {
    return <Navigate to="/perfil/despachador" replace />
  }

  if (tipoUsuario === 'CONDUCTOR') {
    return <Navigate to="/perfil/conductor" replace />
  }

  if (tipoUsuario === 'ADMINISTRADOR') {
    return <Navigate to="/perfil/admin" replace />
  }

  return <Navigate to="/" replace />
}

export default ProfileRedirect
