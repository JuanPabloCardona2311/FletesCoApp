import axios from 'axios'

// Cliente dedicado al microservicio ms-perfil (puerto 8082).
// En despliegue, VITE_PERFIL_URL debe contener la URL pública del microservicio.
const perfilClient = axios.create({
  baseURL: import.meta.env.VITE_PERFIL_URL || 'http://localhost:8082',
})

perfilClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('fleteco_token')

  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }

  return config
})

export default perfilClient
