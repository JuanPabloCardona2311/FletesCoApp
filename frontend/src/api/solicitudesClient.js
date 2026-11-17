import axios from 'axios'

// Cliente dedicado al microservicio ms-solicitudes (puerto 8083).
// Sin VITE_SOLICITUDES_URL (desarrollo local) se llama a http://localhost:8083.
// En el servidor se compila como cadena vacía: las peticiones salen a /api/solicitudes
// en el mismo origen y nginx las reenvía al microservicio. Por eso se usa ?? y
// no ||, que convertiría la cadena vacía otra vez en localhost.
const solicitudesClient = axios.create({
  baseURL: import.meta.env.VITE_SOLICITUDES_URL ?? 'http://localhost:8083',
})

solicitudesClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('fleteco_token')

  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }

  return config
})

export default solicitudesClient
