import axios from 'axios'

const adminClient = axios.create({
  baseURL: 'http://localhost:8084',
})

adminClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('fleteco_token')

  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }

  return config
})

export default adminClient
