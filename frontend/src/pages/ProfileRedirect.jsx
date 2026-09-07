import { Navigate } from 'react-router-dom'

function ProfileRedirect() {
  const tipoUsuario = localStorage.getItem('fleteco_tipo_usuario')

  if (tipoUsuario === 'DESPACHADOR') {
    return <Navigate to="/perfil/despachador" replace />
  }

  return <Navigate to="/perfil/conductor" replace />
}

export default ProfileRedirect
