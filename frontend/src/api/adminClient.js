import axios from 'axios'

// Cliente dedicado al microservicio ms-admin-reportes (puerto 8084).
// En despliegue, VITE_ADMIN_URL debe contener la URL pública del microservicio.
const adminClient = axios.create({
  baseURL: import.meta.env.VITE_ADMIN_URL || 'http://localhost:8084',
})

adminClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('fleteco_token')

  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }

  return config
})

export default adminClient
