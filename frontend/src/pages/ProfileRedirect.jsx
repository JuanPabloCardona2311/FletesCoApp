import { Navigate } from 'react-router-dom'

function ProfileRedirect() {
  const tipoUsuario = localStorage.getItem('fleteco_tipo_usuario')

  if (tipoUsuario === 'DESPACHADOR') {
    return <Navigate to="/perfil/despachador" replace />
  }

  if (tipoUsuario === 'CONDUCTOR') {
    return <Navigate to="/perfil/conductor" replace />
  }

  localStorage.removeItem('fleteco_token')
  localStorage.removeItem('fleteco_tipo_usuario')
  return <Navigate to="/" replace />
}

export default ProfileRedirect
