import axios from 'axios'

// Cliente dedicado al microservicio ms-solicitudes (puerto 8083).
// En despliegue, VITE_SOLICITUDES_URL debe contener la URL pública del microservicio.
const solicitudesClient = axios.create({
  baseURL: import.meta.env.VITE_SOLICITUDES_URL || 'http://localhost:8083',
})

solicitudesClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('fleteco_token')

  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }

  return config
})

export default solicitudesClient
