import axios from 'axios'

// Cliente dedicado al microservicio ms-admin-reportes (puerto 8084).
// Sin VITE_ADMIN_URL (desarrollo local) se llama a http://localhost:8084.
// En el servidor se compila como cadena vacía: las peticiones salen a /api/admin
// en el mismo origen y nginx las reenvía al microservicio. Por eso se usa ?? y
// no ||, que convertiría la cadena vacía otra vez en localhost.
const adminClient = axios.create({
  baseURL: import.meta.env.VITE_ADMIN_URL ?? 'http://localhost:8084',
})

adminClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('fleteco_token')

  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }

  return config
})

export default adminClient
