import axios from 'axios'

// La sesión viaja en la cookie HttpOnly FLETESCO_TOKEN, que pone ms-auth. JavaScript no
// puede leerla (protege el token ante XSS): con withCredentials el navegador la envía sola
// a los microservicios. En local, localhost:5173 y localhost:8081..8084 son el mismo sitio
// para las cookies (no distinguen puerto); en el servidor todo va por el mismo origen.

let alExpirarSesion = null

// SesionContext registra aquí qué hacer cuando el backend responde 401.
export function registrarAlExpirarSesion(manejador) {
  alExpirarSesion = manejador
}

// 401 que son normales y no deben llevar a /login: consultar la sesión sin estar
// conectado, credenciales inválidas en el login y el propio logout.
const RUTAS_SIN_REDIRECCION = ['/api/auth/me', '/api/auth/login', '/api/auth/register', '/api/auth/logout']

export function crearCliente(baseURL) {
  const cliente = axios.create({ baseURL, withCredentials: true })

  cliente.interceptors.response.use(
    (respuesta) => respuesta,
    (error) => {
      const url = error.config?.url || ''
      if (error.response?.status === 401 && !RUTAS_SIN_REDIRECCION.some((ruta) => url.includes(ruta))) {
        alExpirarSesion?.()
      }
      return Promise.reject(error)
    },
  )

  return cliente
}
