import { useState } from 'react'
import { Link } from 'react-router-dom'
import client from '../api/client'
import AuthLayout from '../components/AuthLayout'
import RequestMessage from '../components/RequestMessage'

const initialForm = {
  nombre: '',
  email: '',
  password: '',
  telefono: '',
  tipoDocumentoIdentidad: '',
  numeroDocumentoIdentidad: '',
  tipoUsuario: '',
}

function RegisterPage() {
  const [form, setForm] = useState(initialForm)
  const [mensaje, setMensaje] = useState('')
  const [error, setError] = useState('')
  const [cargando, setCargando] = useState(false)

  const handleChange = (event) => setForm({ ...form, [event.target.name]: event.target.value })

  const handleSubmit = async (event) => {
    event.preventDefault()
    setMensaje('')
    setError('')
    setCargando(true)

    try {
      const response = await client.post('/api/auth/register', form)
      setMensaje(response.data)
      setForm(initialForm)
    } catch (requestError) {
      if (requestError.response?.status === 409) {
        setError('El email ya está registrado.')
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
    <AuthLayout title="Crear cuenta" subtitle="Regístrate para usar la plataforma según tu rol.">
      <form onSubmit={handleSubmit}>
        <label>Nombre completo<input name="nombre" value={form.nombre} onChange={handleChange} required autoComplete="name" /></label>
        <label>Correo electrónico<input type="email" name="email" value={form.email} onChange={handleChange} required autoComplete="email" /></label>
        <label>Contraseña<input type="password" name="password" minLength="6" value={form.password} onChange={handleChange} required autoComplete="new-password" /></label>
        <label>Teléfono<input type="tel" name="telefono" value={form.telefono} onChange={handleChange} autoComplete="tel" /></label>
        <div className="two-columns">
          <label>Tipo de documento<select name="tipoDocumentoIdentidad" value={form.tipoDocumentoIdentidad} onChange={handleChange} required><option value="">Selecciona</option><option value="CC">Cédula de ciudadanía</option><option value="CE">Cédula de extranjería</option><option value="PASAPORTE">Pasaporte</option></select></label>
          <label>Número de documento<input name="numeroDocumentoIdentidad" value={form.numeroDocumentoIdentidad} onChange={handleChange} required /></label>
        </div>
        <fieldset>
          <legend>Tipo de usuario</legend>
          <label className="role-option"><input type="radio" name="tipoUsuario" value="CONDUCTOR" checked={form.tipoUsuario === 'CONDUCTOR'} onChange={handleChange} required />Conductor</label>
          <label className="role-option"><input type="radio" name="tipoUsuario" value="DESPACHADOR" checked={form.tipoUsuario === 'DESPACHADOR'} onChange={handleChange} />Despachador</label>
        </fieldset>
        <button type="submit" disabled={cargando}>{cargando ? 'Registrando...' : 'Crear cuenta'}</button>
        <RequestMessage mensaje={mensaje} error={error} />
      </form>
      <div className="form-footer"><span>¿Ya tienes una cuenta?</span><Link className="link-button" to="/">Iniciar sesión</Link></div>
    </AuthLayout>
  )
}

export default RegisterPage
