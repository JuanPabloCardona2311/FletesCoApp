import { useCallback, useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import authClient from '../api/authClient'
import { registrarAlExpirarSesion } from '../api/sesionHttp'
import { SesionContext } from './useSesion'

// Sesión en memoria: { tipoUsuario, email, nombre } del usuario conectado. El token nunca
// pasa por JavaScript: vive en la cookie HttpOnly y esta información sale de /api/auth/me.

// Claves que la app guardaba antes en localStorage (token y datos del usuario). Se borran
// al arrancar para que no quede un token viejo en navegadores que ya usaban la app.
const CLAVES_ANTIGUAS = ['fleteco_token', 'fleteco_tipo_usuario', 'fleteco_nombre', 'fleteco_email']

const RUTAS_PUBLICAS = ['/', '/login', '/registro', '/recuperar', '/restablecer']

function borrarClavesAntiguas() {
  try {
    CLAVES_ANTIGUAS.forEach((clave) => localStorage.removeItem(clave))
  } catch {
    // Navegador sin acceso a localStorage: no hay nada que limpiar.
  }
}

export function SesionProvider({ children }) {
  const navigate = useNavigate()
  const [usuario, setUsuario] = useState(null)
  const [cargando, setCargando] = useState(true)

  // Pregunta al backend quién es el usuario de la cookie. Devuelve el usuario o null.
  const refrescar = useCallback(async () => {
    try {
      const { data } = await authClient.get('/api/auth/me')
      setUsuario(data)
      return data
    } catch {
      setUsuario(null)
      return null
    }
  }, [])

  useEffect(() => {
    borrarClavesAntiguas()
    refrescar().finally(() => setCargando(false))
  }, [refrescar])

  // Un 401 en cualquier petición: la sesión venció o se cerró en otro lado.
  useEffect(() => {
    registrarAlExpirarSesion(() => {
      setUsuario(null)
      if (!RUTAS_PUBLICAS.includes(window.location.pathname)) {
        navigate('/login', { replace: true })
      }
    })
    return () => registrarAlExpirarSesion(null)
  }, [navigate])

  // Atrás/Adelante puede restaurar la página desde el bfcache del navegador, con la sesión
  // vieja todavía en memoria (por ejemplo, después de "Salir"). Al volver de ahí se
  // consulta otra vez /me: si ya no hay sesión, a /login.
  useEffect(() => {
    const alMostrarPagina = async (event) => {
      if (!event.persisted) return
      const actual = await refrescar()
      if (!actual && !RUTAS_PUBLICAS.includes(window.location.pathname)) {
        navigate('/login', { replace: true })
      }
    }
    window.addEventListener('pageshow', alMostrarPagina)
    return () => window.removeEventListener('pageshow', alMostrarPagina)
  }, [refrescar, navigate])

  const cerrarSesion = useCallback(async () => {
    try {
      await authClient.post('/api/auth/logout')
    } catch {
      // Aunque falle la red, la sesión en memoria se cierra igual.
    }
    setUsuario(null)
    navigate('/login', { replace: true })
  }, [navigate])

  // Datos personales actualizados: el nombre cambia solo en memoria.
  const actualizarNombre = useCallback((nombre) => {
    setUsuario((actual) => (actual ? { ...actual, nombre } : actual))
  }, [])

  const valor = useMemo(() => ({
    usuario,
    cargando,
    // Tras el login o el registro el navegador ya tiene la cookie: se consulta /me.
    iniciarSesion: refrescar,
    cerrarSesion,
    refrescar,
    actualizarNombre,
  }), [usuario, cargando, refrescar, cerrarSesion, actualizarNombre])

  return <SesionContext.Provider value={valor}>{children}</SesionContext.Provider>
}
