import axios from 'axios'

// Cliente dedicado al microservicio ms-auth (puerto 8081).
// En despliegue, VITE_AUTH_URL debe contener la URL pública del microservicio.
const authClient = axios.create({
  baseURL: import.meta.env.VITE_AUTH_URL || 'http://localhost:8081',
})

authClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('fleteco_token')

  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }

  return config
})

export default authClient
