import { useState } from 'react'
import { Link } from 'react-router-dom'
import authClient from '../api/authClient'
import AuthLayout from '../components/AuthLayout'
import RequestMessage from '../components/RequestMessage'

const MENSAJE_GENERICO = 'Si el correo está registrado, recibirás instrucciones para restablecer tu contraseña.'

function RecuperarPasswordPage() {
  const [email, setEmail] = useState('')
  const [mensaje, setMensaje] = useState('')
  const [error, setError] = useState('')
  const [cargando, setCargando] = useState(false)

  const handleSubmit = async (event) => {
    event.preventDefault()
    setMensaje('')
    setError('')
    setCargando(true)

    try {
      const response = await authClient.post('/api/auth/recuperar', { email })
      // El backend responde lo mismo exista o no el correo.
      setMensaje(response.data?.mensaje || MENSAJE_GENERICO)
      setEmail('')
    } catch (requestError) {
      if (requestError.response?.data?.errores?.email) {
        setError(requestError.response.data.errores.email)
      } else if (requestError.response) {
        setError('No fue posible procesar la solicitud. Inténtalo de nuevo.')
      } else {
        setError('No fue posible conectar con el servidor.')
      }
    } finally {
      setCargando(false)
    }
  }

  return (
    <AuthLayout
      title="Recuperar contraseña"
      subtitle="Escribe el correo de tu cuenta y te enviaremos un enlace para crear una contraseña nueva."
    >
      <form onSubmit={handleSubmit}>
        <label>
          Correo electrónico
          <input type="email" name="email" value={email} onChange={(event) => setEmail(event.target.value)} required autoComplete="email" />
        </label>
        <button type="submit" className="btn-accion-principal" disabled={cargando}>
          {cargando ? 'Enviando...' : 'Enviar enlace'}
        </button>
        <RequestMessage mensaje={mensaje} error={error} />
      </form>
      <div className="form-footer">
        <span>¿Recordaste tu contraseña?</span>
        <Link className="link-button" to="/login">Iniciar sesión</Link>
      </div>
    </AuthLayout>
  )
}

export default RecuperarPasswordPage
