import axios from 'axios'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1',
})

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

let isRefreshing = false
let pendingQueue = []

const resolvePending = (token) => {
  pendingQueue.forEach(({ resolve }) => resolve(token))
  pendingQueue = []
}

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config
    const status = error.response?.status
   const isPublicAuthEndpoint = ['/auth/login', '/auth/register', '/auth/refresh'].some((p) =>
  originalRequest.url?.includes(p),
)

if (status !== 401 || originalRequest._retry || isPublicAuthEndpoint) {
  return Promise.reject(error)
}

    if (isRefreshing) {
      return new Promise((resolve) => {
        pendingQueue.push({ resolve })
      }).then((token) => {
        originalRequest.headers.Authorization = `Bearer ${token}`
        return api(originalRequest)
      })
    }

    originalRequest._retry = true
    isRefreshing = true

    try {

      const refreshToken = localStorage.getItem('refreshToken')
      const { data: envelope } = await axios.post(
        `${api.defaults.baseURL}/auth/refresh`,
        {},
        { headers: { Authorization: `Bearer ${refreshToken}` } },
      )
      const data = envelope.data
      localStorage.setItem('accessToken', data.accessToken)
      if (data.refreshToken) localStorage.setItem('refreshToken', data.refreshToken)

      resolvePending(data.accessToken)
      originalRequest.headers.Authorization = `Bearer ${data.accessToken}`
      return api(originalRequest)
    } catch (refreshError) {
      localStorage.removeItem('accessToken')
      localStorage.removeItem('refreshToken')
      window.location.href = '/login'
      return Promise.reject(refreshError)
    } finally {
      isRefreshing = false
    }
  },
)

export default api
