import axios from 'axios'

// Cliente dedicado al microservicio ms-auth (puerto 8081).
// Sin VITE_AUTH_URL (desarrollo local) se llama a http://localhost:8081.
// En el servidor se compila como cadena vacía: las peticiones salen a /api/auth
// en el mismo origen y nginx las reenvía al microservicio. Por eso se usa ?? y
// no ||, que convertiría la cadena vacía otra vez en localhost.
const authClient = axios.create({
  baseURL: import.meta.env.VITE_AUTH_URL ?? 'http://localhost:8081',
})

authClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('fleteco_token')

  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }

  return config
})

export default authClient
