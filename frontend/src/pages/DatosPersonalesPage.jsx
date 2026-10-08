import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import perfilClient from '../api/perfilClient'
import ProfileLayout from '../components/ProfileLayout'
import RequestMessage from '../components/RequestMessage'

const initialForm = {
  nombre: '',
  telefono: '',
}

function DatosPersonalesPage() {
  const navigate = useNavigate()
  const tipoUsuario = localStorage.getItem('fleteco_tipo_usuario')
  const role = tipoUsuario === 'DESPACHADOR' ? 'Perfil de despachador' : 'Perfil de conductor'
  const [form, setForm] = useState(initialForm)
  const [email, setEmail] = useState('')
  const [errors, setErrors] = useState({})
  const [mensaje, setMensaje] = useState('')
  const [error, setError] = useState('')
  const [cargando, setCargando] = useState(false)

  const cerrarSesionSiExpiro = (requestError) => {
    if (requestError.response?.status === 401) {
      localStorage.removeItem('fleteco_token')
      localStorage.removeItem('fleteco_tipo_usuario')
      navigate('/')
      return true
    }
    return false
  }

  useEffect(() => {
    const cargarDatos = async () => {
      try {
        const response = await perfilClient.get('/api/perfiles/datos')
        setForm({
          nombre: response.data.nombre || '',
          telefono: response.data.telefono || '',
        })
        setEmail(response.data.email || '')
      } catch (requestError) {
        if (!cerrarSesionSiExpiro(requestError)) {
          setError(requestError.response?.data?.error || 'No fue posible cargar tus datos personales.')
        }
      }
    }

    cargarDatos()
  }, [navigate])

  const handleChange = (event) => {
    const { name, value } = event.target
    setForm((prev) => ({ ...prev, [name]: value }))
    setErrors((prev) => ({ ...prev, [name]: '' }))
  }

  const validate = () => {
    const validationErrors = {}
    const nombre = form.nombre.trim()
    const telefono = form.telefono.trim()

    if (!nombre) {
      validationErrors.nombre = 'El nombre es obligatorio'
    } else if (!/^[\p{L} ]{3,100}$/u.test(nombre)) {
      validationErrors.nombre = 'Entre 3 y 100 caracteres, solo letras y espacios'
    }

    if (!telefono) {
      validationErrors.telefono = 'El teléfono es obligatorio'
    } else if (!/^3[0-9]{9}$/.test(telefono)) {
      validationErrors.telefono = 'Debe ser un celular colombiano válido (10 dígitos, inicia en 3)'
    }

    return validationErrors
  }

  const handleSubmit = async (event) => {
    event.preventDefault()
    setMensaje('')
    setError('')

    const validationErrors = validate()
    setErrors(validationErrors)
    if (Object.keys(validationErrors).length > 0) return

    setCargando(true)
    try {
      const response = await perfilClient.put('/api/perfiles/datos', {
        nombre: form.nombre.trim(),
        telefono: form.telefono.trim(),
      })
      setForm({
        nombre: response.data.nombre || '',
        telefono: response.data.telefono || '',
      })
      setEmail(response.data.email || '')
      setMensaje('Datos actualizados correctamente')
      if (localStorage.getItem('fleteco_nombre')) {
        localStorage.setItem('fleteco_nombre', response.data.nombre)
      }
    } catch (requestError) {
      if (cerrarSesionSiExpiro(requestError)) return
      if (requestError.response?.data?.errores) {
        setErrors(requestError.response.data.errores)
        setError('Por favor verifica los campos marcados.')
      } else {
        setError(requestError.response?.data?.error || 'No fue posible actualizar tus datos personales.')
      }
    } finally {
      setCargando(false)
    }
  }

  return (
    <ProfileLayout
      role={role}
      title="Datos personales"
      subtitle="Actualiza tu nombre y teléfono de contacto."
    >
      <div className="profile-grid">
        <div className="conductor-main">
          <RequestMessage mensaje={mensaje} error={error} />
          <section className="profile-panel">
            <form onSubmit={handleSubmit} noValidate>
              <div className="two-columns">
                <label>
                  Nombre completo
                  <input
                    name="nombre"
                    value={form.nombre}
                    onChange={handleChange}
                    maxLength="100"
                    autoComplete="name"
                  />
                  {errors.nombre && <span className="field-error" role="alert">{errors.nombre}</span>}
                </label>
                <label>
                  Teléfono celular
                  <input
                    type="tel"
                    name="telefono"
                    value={form.telefono}
                    onChange={handleChange}
                    maxLength="10"
                    autoComplete="tel"
                  />
                  {errors.telefono && <span className="field-error" role="alert">{errors.telefono}</span>}
                </label>
              </div>
              <label>
                Correo electrónico
                <input type="email" value={email} readOnly />
                <small>El correo es tu usuario de acceso y no se puede cambiar</small>
              </label>
              <button type="submit" disabled={cargando}>
                {cargando ? 'Guardando...' : 'Guardar cambios'}
              </button>
            </form>
          </section>
          <Link className="link-button" to="/perfil">Volver a mi perfil</Link>
        </div>
      </div>
    </ProfileLayout>
  )
}

export default DatosPersonalesPage
