import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import authClient from '../api/authClient'
import AuthLayout from '../components/AuthLayout'
import RequestMessage from '../components/RequestMessage'
import { useSesion } from '../context/useSesion'

const initialForm = { email: '', password: '' }

function LoginPage() {
  const navigate = useNavigate()
  const { iniciarSesion } = useSesion()
  const [form, setForm] = useState(initialForm)
  const [mensaje, setMensaje] = useState('')
  const [error, setError] = useState('')
  const [cargando, setCargando] = useState(false)

  const handleSubmit = async (event) => {
    event.preventDefault()
    setMensaje('')
    setError('')
    setCargando(true)

    try {
      // El backend deja el token en la cookie HttpOnly; aquí no se guarda nada.
      await authClient.post('/api/auth/login', form)
      const usuario = await iniciarSesion()
      if (!usuario) {
        setError('No fue posible iniciar la sesión. Revisa que el navegador acepte cookies.')
        return
      }

      setMensaje(`Bienvenido. Rol: ${usuario.tipoUsuario.toLowerCase()}.`)
      setForm(initialForm)

      if (usuario.tipoUsuario === 'ADMINISTRADOR') {
        navigate('/perfil/admin')
      } else if (usuario.tipoUsuario === 'DESPACHADOR') {
        navigate('/perfil/despachador')
      } else {
        navigate('/perfil/conductor')
      }
    } catch (requestError) {
      if (requestError.response?.status === 401) {
        setError('El correo o la contraseña no son válidos.')
      } else if (requestError.response?.status === 403) {
        setError(requestError.response?.data?.error || 'Tu cuenta está inactiva. Contacta al administrador.')
      } else if (requestError.response?.data) {
        setError(typeof requestError.response.data === 'string' ? requestError.response.data : 'Revisa los datos ingresados.')
      } else {
        setError('No fue posible conectar con el servidor.')
      }
    } finally {
      setCargando(false)
    }
  }

  return (
    <AuthLayout title="Iniciar sesión" subtitle="Accede a tu espacio de trabajo en FletesCo.">
      <form onSubmit={handleSubmit}>
        <label>
          Correo electrónico
          <input type="email" name="email" value={form.email} onChange={(event) => setForm({ ...form, email: event.target.value })} required autoComplete="email" />
        </label>
        <label>
          Contraseña
          <input type="password" name="password" value={form.password} onChange={(event) => setForm({ ...form, password: event.target.value })} required autoComplete="current-password" />
        </label>
        <Link className="link-olvido-password" to="/recuperar">¿Olvidaste tu contraseña?</Link>
        <button type="submit" className="btn-accion-principal" disabled={cargando}>{cargando ? 'Ingresando...' : 'Iniciar sesión'}</button>
        <RequestMessage mensaje={mensaje} error={error} />
      </form>
      <div className="form-footer">
        <span>¿Aún no tienes una cuenta?</span>
        <Link className="link-button" to="/registro">Crear cuenta</Link>
      </div>
    </AuthLayout>
  )
}

export default LoginPage
