import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import client from '../api/client'
import AuthLayout from '../components/AuthLayout'
import RequestMessage from '../components/RequestMessage'

const initialForm = { email: '', password: '' }

function LoginPage() {
  const navigate = useNavigate()
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
      const response = await client.post('/api/auth/login', form)
      localStorage.setItem('fleteco_token', response.data.token)
      localStorage.setItem('fleteco_tipo_usuario', response.data.tipoUsuario)
      setMensaje(`Bienvenido. Rol: ${response.data.tipoUsuario.toLowerCase()}.`)
      setForm(initialForm)
      navigate(response.data.tipoUsuario === 'DESPACHADOR' ? '/perfil/despachador' : '/perfil/conductor')
    } catch (requestError) {
      if (requestError.response?.status === 401) {
        setError('El correo o la contraseña no son válidos.')
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
    <AuthLayout title="Iniciar sesión" subtitle="Accede a tu espacio de trabajo en FleteCo.">
      <form onSubmit={handleSubmit}>
        <label>
          Correo electrónico
          <input type="email" name="email" value={form.email} onChange={(event) => setForm({ ...form, email: event.target.value })} required autoComplete="email" />
        </label>
        <label>
          Contraseña
          <input type="password" name="password" value={form.password} onChange={(event) => setForm({ ...form, password: event.target.value })} required autoComplete="current-password" />
        </label>
        <button type="submit" disabled={cargando}>{cargando ? 'Ingresando...' : 'Iniciar sesión'}</button>
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
