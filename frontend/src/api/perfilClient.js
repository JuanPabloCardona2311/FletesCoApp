import axios from 'axios'

// Cliente dedicado al microservicio ms-perfil (puerto 8082).
// Sin VITE_PERFIL_URL (desarrollo local) se llama a http://localhost:8082.
// En el servidor se compila como cadena vacía: las peticiones salen a /api/perfiles
// en el mismo origen y nginx las reenvía al microservicio. Por eso se usa ?? y
// no ||, que convertiría la cadena vacía otra vez en localhost.
const perfilClient = axios.create({
  baseURL: import.meta.env.VITE_PERFIL_URL ?? 'http://localhost:8082',
})

perfilClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('fleteco_token')

  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }

  return config
})

export default perfilClient
