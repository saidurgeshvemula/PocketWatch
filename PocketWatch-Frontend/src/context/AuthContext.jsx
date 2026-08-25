import { createContext, useContext, useState, useCallback } from 'react'
import api from '@/api/axios'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {

  const [user, setUser] = useState(() => {
    const stored = localStorage.getItem('user')
    return stored ? JSON.parse(stored) : null
  })

  const persistSession = useCallback((data) => {
    localStorage.setItem('accessToken', data.accessToken)
    localStorage.setItem('refreshToken', data.refreshToken)

    if (data.user) {
      localStorage.setItem('user', JSON.stringify(data.user))
      setUser(data.user)
    }
  }, [])

  const login = useCallback(async ({ email, password }) => {

    const { data: envelope } = await api.post('/auth/login', {
      email,
      password,
    })

    persistSession(envelope.data)

    return envelope.data

  }, [persistSession])

  const register = useCallback(async ({ fullName, email, password }) => {

    const { data: envelope } = await api.post('/auth/register', {
      fullName,
      email,
      password,
    })

    return envelope.data

  }, [])

  const updateUser = useCallback((patch) => {

    setUser((prev) => {

      const next = { ...prev, ...patch }

      localStorage.setItem('user', JSON.stringify(next))

      return next
    })

  }, [])

  const logout = useCallback(() => {

    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('user')

    setUser(null)

  }, [])

  const isAuthenticated = Boolean(localStorage.getItem('accessToken'))

  return (
    <AuthContext.Provider
      value={{
        user,
        login,
        register,
        logout,
        isAuthenticated,
        updateUser,
      }}
    >
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {

  const ctx = useContext(AuthContext)

  if (!ctx) {
    throw new Error('useAuth must be used within AuthProvider')
  }

  return ctx
}