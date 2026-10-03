import axios from 'axios'

const perfilClient = axios.create({
  baseURL: 'http://localhost:8082',
})

perfilClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('fleteco_token')

  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }

  return config
})

export default perfilClient
