import { useEffect, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import authClient from '../api/authClient'
import AuthLayout from '../components/AuthLayout'
import RequestMessage from '../components/RequestMessage'

const LONGITUD_MINIMA = 8
const SEGUNDOS_ANTES_DE_IR_AL_LOGIN = 3

function RestablecerPasswordPage() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const token = searchParams.get('token') || ''

  const [form, setForm] = useState({ nuevaPassword: '', confirmacion: '' })
  const [mensaje, setMensaje] = useState('')
  const [error, setError] = useState('')
  const [cargando, setCargando] = useState(false)

  useEffect(() => {
    if (!mensaje) return undefined
    const temporizador = setTimeout(() => navigate('/login'), SEGUNDOS_ANTES_DE_IR_AL_LOGIN * 1000)
    return () => clearTimeout(temporizador)
  }, [mensaje, navigate])

  const handleSubmit = async (event) => {
    event.preventDefault()
    setMensaje('')
    setError('')

    if (form.nuevaPassword.length < LONGITUD_MINIMA) {
      setError(`La contraseña debe tener al menos ${LONGITUD_MINIMA} caracteres.`)
      return
    }
    if (form.nuevaPassword !== form.confirmacion) {
      setError('Las contraseñas no coinciden.')
      return
    }

    setCargando(true)
    try {
      const response = await authClient.post('/api/auth/restablecer', {
        token,
        nuevaPassword: form.nuevaPassword,
      })
      setForm({ nuevaPassword: '', confirmacion: '' })
      setMensaje(`${response.data?.mensaje || 'Tu contraseña se actualizó.'} Te llevaremos al inicio de sesión...`)
    } catch (requestError) {
      const data = requestError.response?.data
      if (data?.errores) {
        setError(Object.values(data.errores)[0])
      } else if (data?.error) {
        setError(data.error)
      } else if (requestError.response) {
        setError('No fue posible restablecer la contraseña. Inténtalo de nuevo.')
      } else {
        setError('No fue posible conectar con el servidor.')
      }
    } finally {
      setCargando(false)
    }
  }

  if (!token) {
    return (
      <AuthLayout title="Enlace no válido" subtitle="Este enlace de recuperación está incompleto.">
        <RequestMessage error="Abre el enlace completo que recibiste o solicita uno nuevo." />
        <div className="form-footer">
          <Link className="link-button" to="/recuperar">Solicitar un enlace nuevo</Link>
        </div>
      </AuthLayout>
    )
  }

  return (
    <AuthLayout title="Nueva contraseña" subtitle="Elige una contraseña nueva para tu cuenta.">
      <form onSubmit={handleSubmit}>
        <label>
          Nueva contraseña
          <input
            type="password"
            name="nuevaPassword"
            value={form.nuevaPassword}
            onChange={(event) => setForm({ ...form, nuevaPassword: event.target.value })}
            required
            minLength={LONGITUD_MINIMA}
            autoComplete="new-password"
            disabled={Boolean(mensaje)}
          />
        </label>
        <label>
          Confirmar contraseña
          <input
            type="password"
            name="confirmacion"
            value={form.confirmacion}
            onChange={(event) => setForm({ ...form, confirmacion: event.target.value })}
            required
            autoComplete="new-password"
            disabled={Boolean(mensaje)}
          />
        </label>
        <button type="submit" className="btn-accion-principal" disabled={cargando || Boolean(mensaje)}>
          {cargando ? 'Guardando...' : 'Guardar contraseña'}
        </button>
        <RequestMessage mensaje={mensaje} error={error} />
      </form>
      <div className="form-footer">
        {error ? (
          <Link className="link-button" to="/recuperar">Solicitar un enlace nuevo</Link>
        ) : (
          <Link className="link-button" to="/login">Volver a iniciar sesión</Link>
        )}
      </div>
    </AuthLayout>
  )
}

export default RestablecerPasswordPage
