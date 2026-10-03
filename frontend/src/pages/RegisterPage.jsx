import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import authClient from '../api/authClient'
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
  const navigate = useNavigate()
  const [form, setForm] = useState(initialForm)
  const [errors, setErrors] = useState({})
  const [mensaje, setMensaje] = useState('')
  const [error, setError] = useState('')
  const [cargando, setCargando] = useState(false)

  const handleChange = (event) => {
    const { name, value } = event.target
    setForm((prev) => ({ ...prev, [name]: value }))
    if (errors[name]) {
      setErrors((prev) => ({ ...prev, [name]: '' }))
    }
  }

  const validate = () => {
    const newErrors = {}

    // E1-06: Validar longitud (3-100) y solo letras/espacios en nombre
    if (!form.nombre.trim()) {
      newErrors.nombre = 'El nombre es obligatorio'
    } else if (!/^[\p{L} ]{3,100}$/u.test(form.nombre.trim())) {
      newErrors.nombre = 'Entre 3 y 100 caracteres, solo letras y espacios'
    }

    // E1-10: Validar email y longitud máxima 150
    if (!form.email.trim()) {
      newErrors.email = 'El email es obligatorio'
    } else if (form.email.length > 150) {
      newErrors.email = 'Máximo 150 caracteres'
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email.trim())) {
      newErrors.email = 'Debe ser un email válido'
    }

    // E1-09: Exigir contraseña con longitud mínima de 8 caracteres
    if (!form.password) {
      newErrors.password = 'La contraseña es obligatoria'
    } else if (form.password.length < 8) {
      newErrors.password = 'La contraseña debe tener al menos 8 caracteres'
    }

    // E1-07: Validar formato de teléfono celular colombiano (10 dígitos, inicia en 3)
    if (!form.telefono.trim()) {
      newErrors.telefono = 'El teléfono es obligatorio'
    } else if (!/^3[0-9]{9}$/.test(form.telefono.trim())) {
      newErrors.telefono = 'Debe ser un celular colombiano válido (10 dígitos, inicia en 3)'
    }

    // Tipo de documento
    if (!form.tipoDocumentoIdentidad) {
      newErrors.tipoDocumentoIdentidad = 'Debe indicar el tipo de documento'
    }

    // E1-08: Validar documento de identidad (solo dígitos, 5-20 caracteres)
    if (!form.numeroDocumentoIdentidad.trim()) {
      newErrors.numeroDocumentoIdentidad = 'El número de documento es obligatorio'
    } else if (!/^[0-9]{5,20}$/.test(form.numeroDocumentoIdentidad.trim())) {
      newErrors.numeroDocumentoIdentidad = 'Solo números, entre 5 y 20 dígitos'
    }

    // Tipo de usuario
    if (!form.tipoUsuario) {
      newErrors.tipoUsuario = 'Debe indicar el tipo de usuario'
    }

    return newErrors
  }

  const handleSubmit = async (event) => {
    event.preventDefault()
    setMensaje('')
    setError('')

    const validationErrors = validate()
    setErrors(validationErrors)

    if (Object.keys(validationErrors).length > 0) {
      return
    }

    setCargando(true)

    try {
      const response = await authClient.post('/api/auth/register', form)
      const data = response.data

      // E1-13: Autenticación automática y redirección
      localStorage.setItem('fleteco_token', data.token)
      localStorage.setItem('fleteco_tipo_usuario', data.tipoUsuario)

      setMensaje(`Registro exitoso. Bienvenido, ${data.nombre || form.nombre}.`)
      setForm(initialForm)

      navigate(data.tipoUsuario === 'DESPACHADOR' ? '/perfil/despachador' : '/perfil/conductor')
    } catch (requestError) {
      if (requestError.response?.status === 409) {
        setError('El email ya está registrado.')
      } else if (requestError.response?.data?.errores) {
        setErrors((prev) => ({ ...prev, ...requestError.response.data.errores }))
        setError('Por favor verifica los campos marcados.')
      } else if (requestError.response?.data?.error) {
        setError(requestError.response.data.error)
      } else if (typeof requestError.response?.data === 'string') {
        setError(requestError.response.data)
      } else {
        setError('No fue posible conectar con el servidor.')
      }
    } finally {
      setCargando(false)
    }
  }

  return (
    <AuthLayout title="Crear cuenta" subtitle="Regístrate para usar la plataforma según tu rol.">
      <form onSubmit={handleSubmit} noValidate>
        <label>
          Nombre completo
          <input
            name="nombre"
            value={form.nombre}
            onChange={handleChange}
            required
            autoComplete="name"
          />
          {errors.nombre && <span className="field-error">{errors.nombre}</span>}
        </label>

        <label>
          Correo electrónico
          <input
            type="email"
            name="email"
            value={form.email}
            onChange={handleChange}
            required
            autoComplete="email"
          />
          {errors.email && <span className="field-error">{errors.email}</span>}
        </label>

        <label>
          Contraseña
          <input
            type="password"
            name="password"
            value={form.password}
            onChange={handleChange}
            required
            autoComplete="new-password"
          />
          {errors.password && <span className="field-error">{errors.password}</span>}
        </label>

        <label>
          Teléfono celular
          <input
            type="tel"
            name="telefono"
            placeholder="Ej: 3001234567"
            value={form.telefono}
            onChange={handleChange}
            autoComplete="tel"
          />
          {errors.telefono && <span className="field-error">{errors.telefono}</span>}
        </label>

        <div className="two-columns">
          <label>
            Tipo de documento
            <select
              name="tipoDocumentoIdentidad"
              value={form.tipoDocumentoIdentidad}
              onChange={handleChange}
              required
            >
              <option value="">Selecciona</option>
              <option value="CC">Cédula de ciudadanía</option>
              <option value="CE">Cédula de extranjería</option>
              <option value="PASAPORTE">Pasaporte</option>
            </select>
            {errors.tipoDocumentoIdentidad && (
              <span className="field-error">{errors.tipoDocumentoIdentidad}</span>
            )}
          </label>

          <label>
            Número de documento
            <input
              name="numeroDocumentoIdentidad"
              value={form.numeroDocumentoIdentidad}
              onChange={handleChange}
              required
            />
            {errors.numeroDocumentoIdentidad && (
              <span className="field-error">{errors.numeroDocumentoIdentidad}</span>
            )}
          </label>
        </div>

        <fieldset>
          <legend>Tipo de usuario</legend>
          <label className="role-option">
            <input
              type="radio"
              name="tipoUsuario"
              value="CONDUCTOR"
              checked={form.tipoUsuario === 'CONDUCTOR'}
              onChange={handleChange}
              required
            />
            Conductor
          </label>
          <label className="role-option">
            <input
              type="radio"
              name="tipoUsuario"
              value="DESPACHADOR"
              checked={form.tipoUsuario === 'DESPACHADOR'}
              onChange={handleChange}
            />
            Despachador
          </label>
          {errors.tipoUsuario && <span className="field-error">{errors.tipoUsuario}</span>}
        </fieldset>

        <button type="submit" disabled={cargando}>
          {cargando ? 'Registrando...' : 'Crear cuenta'}
        </button>

        <RequestMessage mensaje={mensaje} error={error} />
      </form>

      <div className="form-footer">
        <span>¿Ya tienes una cuenta?</span>
        <Link className="link-button" to="/login">
          Iniciar sesión
        </Link>
      </div>
    </AuthLayout>
  )
}

export default RegisterPage

