import axios from 'axios'

const solicitudesClient = axios.create({
  baseURL: 'http://localhost:8083',
})

solicitudesClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('fleteco_token')

  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }

  return config
})

export default solicitudesClient
