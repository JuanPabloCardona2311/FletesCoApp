import { useEffect, useId, useRef, useState } from 'react'
import { buscarDirecciones } from '../api/geocoderService'
import './BuscadorDireccion.css'

const MINIMO_CARACTERES = 4
const ESPERA_ESCRITURA_MS = 600
// Nominatim permite como máximo 1 petición por segundo.
const ESPACIO_ENTRE_PETICIONES_MS = 1000

/**
 * BuscadorDireccion: campo de texto con sugerencias de direcciones (Nominatim).
 *
 * Props:
 *   etiqueta        – texto del label ("Origen", "Destino")
 *   valor           – texto actual (lo controla el padre)
 *   onCambiarTexto(texto)          – el usuario escribió
 *   onElegir({ label, lat, lng })  – el usuario eligió una sugerencia
 *   placeholder, error
 */
function BuscadorDireccion({ etiqueta, valor, onCambiarTexto, onElegir, placeholder, error }) {
  const id = useId()
  const idLista = `${id}-lista`
  // Solo lo que el usuario escribe dispara la búsqueda; si el texto cambia por el
  // mapa (reverse) o por elegir una sugerencia, no se busca.
  const [consulta, setConsulta] = useState('')
  const [sugerencias, setSugerencias] = useState([])
  const [estado, setEstado] = useState('inactivo') // inactivo | buscando | listo | error
  const [abierto, setAbierto] = useState(false)
  const [activo, setActivo] = useState(-1)
  const ultimaPeticion = useRef(0)

  const consultaValida = consulta.trim().length >= MINIMO_CARACTERES

  useEffect(() => {
    const texto = consulta.trim()
    if (texto.length < MINIMO_CARACTERES) return undefined

    // Cada cambio cancela la petición anterior: no llegan respuestas viejas.
    const controlador = new AbortController()
    const espera = Math.max(
      ESPERA_ESCRITURA_MS,
      ESPACIO_ENTRE_PETICIONES_MS - (Date.now() - ultimaPeticion.current),
    )
    const temporizador = setTimeout(async () => {
      ultimaPeticion.current = Date.now()
      setEstado('buscando')
      try {
        const resultados = await buscarDirecciones(texto, controlador.signal)
        setSugerencias(resultados)
        setActivo(-1)
        setEstado('listo')
      } catch (err) {
        if (err.name !== 'AbortError') setEstado('error')
      }
    }, espera)

    return () => {
      clearTimeout(temporizador)
      controlador.abort()
    }
  }, [consulta])

  const cambiarTexto = (event) => {
    const texto = event.target.value
    onCambiarTexto(texto)
    setConsulta(texto)
    setSugerencias([])
    setEstado('inactivo')
    setAbierto(true)
  }

  const elegir = (sugerencia) => {
    onElegir(sugerencia)
    setConsulta('')
    setSugerencias([])
    setEstado('inactivo')
    setAbierto(false)
    setActivo(-1)
  }

  const manejarTeclado = (event) => {
    const hayOpciones = abierto && sugerencias.length > 0
    if (event.key === 'ArrowDown' && hayOpciones) {
      event.preventDefault()
      setActivo((i) => (i + 1) % sugerencias.length)
    } else if (event.key === 'ArrowUp' && hayOpciones) {
      event.preventDefault()
      setActivo((i) => (i <= 0 ? sugerencias.length - 1 : i - 1))
    } else if (event.key === 'Enter' && hayOpciones && activo >= 0) {
      // Sin esto, Enter enviaría el formulario de la solicitud.
      event.preventDefault()
      elegir(sugerencias[activo])
    } else if (event.key === 'Escape' && abierto) {
      event.preventDefault()
      setAbierto(false)
    }
  }

  const mostrarPanel = abierto && consultaValida && estado !== 'inactivo'
  const mostrarLista = mostrarPanel && estado === 'listo' && sugerencias.length > 0
  const idOpcion = (indice) => `${id}-opcion-${indice}`

  return (
    <div className="buscador-direccion">
      <label htmlFor={id}>{etiqueta}</label>
      <input
        id={id}
        type="text"
        role="combobox"
        aria-autocomplete="list"
        aria-expanded={mostrarLista}
        aria-controls={mostrarLista ? idLista : undefined}
        aria-activedescendant={mostrarLista && activo >= 0 ? idOpcion(activo) : undefined}
        autoComplete="off"
        value={valor}
        onChange={cambiarTexto}
        onKeyDown={manejarTeclado}
        onBlur={() => setAbierto(false)}
        onFocus={() => consultaValida && setAbierto(true)}
        placeholder={placeholder}
        maxLength="200"
      />

      {mostrarPanel && (
        <div className="buscador-direccion__panel">
          {estado === 'buscando' && <p className="buscador-direccion__estado" role="status">Buscando…</p>}
          {estado === 'error' && (
            <p className="buscador-direccion__estado buscador-direccion__estado--error" role="alert">
              No se pudo buscar la dirección. Intenta de nuevo o marca el punto en el mapa.
            </p>
          )}
          {estado === 'listo' && sugerencias.length === 0 && (
            <p className="buscador-direccion__estado" role="status">Sin resultados</p>
          )}
          {mostrarLista && (
            <ul id={idLista} role="listbox" aria-label={`Sugerencias de ${etiqueta.toLowerCase()}`}
              className="buscador-direccion__lista">
              {sugerencias.map((sugerencia, indice) => (
                <li
                  key={`${sugerencia.lat},${sugerencia.lng},${indice}`}
                  id={idOpcion(indice)}
                  role="option"
                  aria-selected={indice === activo}
                  className={`buscador-direccion__opcion${indice === activo ? ' buscador-direccion__opcion--activa' : ''}`}
                  // mousedown y no click: así el blur del input no cierra la lista antes de elegir.
                  onMouseDown={(event) => {
                    event.preventDefault()
                    elegir(sugerencia)
                  }}
                  onMouseEnter={() => setActivo(indice)}
                >
                  {sugerencia.label}
                </li>
              ))}
            </ul>
          )}
        </div>
      )}

      {error && <span className="field-error" role="alert">{error}</span>}
    </div>
  )
}

export default BuscadorDireccion
