// Canales WebSocket con reconexión automática, compartidos entre componentes.
// El WebSocket solo avisa que algo cambió: los datos siempre se vuelven a pedir por HTTP.

const ESPERA_INICIAL_MS = 1000
const ESPERA_MAXIMA_MS = 30000
// Cloudflare corta las conexiones inactivas a los ~100 s: un PING cada 25 s las mantiene.
const INTERVALO_PING_MS = 25000

const canales = new Map()

// Convierte la base HTTP de un microservicio en la URL del WebSocket, con el token.
// Base vacía (servidor): mismo origen de la página, http: -> ws: y https: -> wss:.
export function urlWebSocket(baseHttp, ruta) {
  const base = baseHttp === '' ? window.location.origin : baseHttp
  const baseWs = base.replace(/^http(s?):/, 'ws$1:')
  const token = localStorage.getItem('fleteco_token') ?? ''
  return `${baseWs}${ruta}?token=${encodeURIComponent(token)}`
}

// Devuelve el canal de esa URL; varios componentes comparten una sola conexión.
export function crearCanal(baseHttp, ruta) {
  const clave = `${baseHttp}${ruta}`
  if (!canales.has(clave)) {
    canales.set(clave, nuevoCanal(baseHttp, ruta))
  }
  return canales.get(clave)
}

function nuevoCanal(baseHttp, ruta) {
  const oyentes = new Set()
  const oyentesEstado = new Set()
  let socket = null
  let temporizadorReconexion = null
  let intervaloPing = null
  let espera = ESPERA_INICIAL_MS
  let estado = 'reconectando'

  const cambiarEstado = (nuevo) => {
    if (estado === nuevo) return
    estado = nuevo
    oyentesEstado.forEach((callback) => callback(nuevo))
  }

  const programarReconexion = () => {
    if (oyentes.size === 0 || temporizadorReconexion) return
    temporizadorReconexion = setTimeout(() => {
      temporizadorReconexion = null
      conectar()
    }, espera)
    espera = Math.min(espera * 2, ESPERA_MAXIMA_MS)
  }

  const conectar = () => {
    if (socket || oyentes.size === 0) return
    // Sin sesión no se conecta (por ejemplo, después de cerrar sesión).
    if (!localStorage.getItem('fleteco_token')) return

    socket = new WebSocket(urlWebSocket(baseHttp, ruta))
    socket.onopen = () => {
      espera = ESPERA_INICIAL_MS
      cambiarEstado('conectado')
      intervaloPing = setInterval(() => {
        if (socket?.readyState === WebSocket.OPEN) socket.send('{"tipo":"PING"}')
      }, INTERVALO_PING_MS)
    }
    socket.onmessage = (evento) => {
      let mensaje
      try {
        mensaje = JSON.parse(evento.data)
      } catch {
        return
      }
      if (!mensaje || mensaje.tipo === 'PONG') return
      oyentes.forEach((callback) => callback(mensaje))
    }
    socket.onclose = () => {
      clearInterval(intervaloPing)
      intervaloPing = null
      socket = null
      cambiarEstado('reconectando')
      programarReconexion()
    }
  }

  const cerrar = () => {
    clearTimeout(temporizadorReconexion)
    temporizadorReconexion = null
    clearInterval(intervaloPing)
    intervaloPing = null
    if (socket) {
      socket.onclose = null
      socket.close()
      socket = null
    }
    espera = ESPERA_INICIAL_MS
    cambiarEstado('reconectando')
  }

  return {
    get estado() {
      return estado
    },
    // Recibe cada mensaje del servidor. Devuelve la función para dejar de escuchar;
    // al irse el último oyente se cierra la conexión.
    escuchar(callback) {
      oyentes.add(callback)
      conectar()
      return () => {
        oyentes.delete(callback)
        if (oyentes.size === 0) cerrar()
      }
    },
    // Avisa 'conectado' o 'reconectando'; llama de inmediato con el estado actual.
    alCambiarEstado(callback) {
      oyentesEstado.add(callback)
      callback(estado)
      return () => oyentesEstado.delete(callback)
    },
  }
}
