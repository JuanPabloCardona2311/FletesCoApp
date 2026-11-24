// Geocoding con Nominatim (OpenStreetMap): dirección → coordenadas y al revés.
// Política de uso de Nominatim: máximo 1 petición por segundo. Quien llama a estas
// funciones se encarga del debounce y la espera entre peticiones.

const NOMINATIM_URL = 'https://nominatim.openstreetmap.org'

// Valle de Aburrá aproximado (oeste, norte, este, sur). Solo da prioridad a Medellín y
// su área metropolitana: sin bounded=1 no excluye direcciones de otros municipios.
const VIEWBOX_VALLE_DE_ABURRA = '-75.72,6.45,-75.30,6.00'

const LARGO_MAXIMO_TEXTO = 200

const ABREVIATURAS_VIA = [
  [/^(cra|kra|kr|cr)\.?$/i, 'Carrera'],
  [/^(cl|cll)\.?$/i, 'Calle'],
  [/^(dg|diag)\.?$/i, 'Diagonal'],
  [/^(tv|tr|transv)\.?$/i, 'Transversal'],
  [/^(av|avda)\.?$/i, 'Avenida'],
  [/^(cq|circ)\.?$/i, 'Circular'],
]

// OpenStreetMap no tiene placas en Medellín: Nominatim solo encuentra la vía, y una
// placa como "#39-45" hace que no devuelva nada. Además separa "65 E" en "65" y "E" y
// trae otra calle (Carrera 65 en vez de 65E). Por eso se busca solo la vía, con número
// y letra juntos y abreviaturas expandidas, y la placa se guarda aparte para la etiqueta.
export function normalizarConsulta(texto) {
  const patronPlaca = /\s*(?:#|n[°ºo]\.?\s)\s*([0-9]+\s*[a-z]?\s*-\s*[0-9]+\s*[a-z]?)/i
  const coincidencia = texto.match(patronPlaca)
  const placa = coincidencia ? `#${coincidencia[1].replace(/\s+/g, '').toUpperCase()}` : ''
  const sinPlaca = texto.replace(patronPlaca, '')
  const [primera = '', ...resto] = sinPlaca.trim().split(/\s+/)
  const via = ABREVIATURAS_VIA.find(([patron]) => patron.test(primera))
  const consulta = [via ? via[1] : primera, ...resto].join(' ')
    .replace(/(\d+)\s+([a-z])\b/gi, (_, numero, letra) => `${numero}${letra.toUpperCase()}`)
    .replace(/\s+,/g, ',')
    .trim()
  return { consulta, placa }
}

// Arma un texto corto: vía (con la placa que escribió el usuario si Nominatim no trae
// número), barrio, ciudad y departamento. El nombre completo de Nominatim suele pasar del
// máximo del campo (200 caracteres).
function etiquetaCorta(resultado, placa) {
  const a = resultado.address || {}
  const numero = a.house_number || placa
  const calle = a.road ? [a.road, numero].filter(Boolean).join(' ') : ''
  const ciudad = (a.city || a.town || a.village || a.municipality || '').replace(/^Perímetro Urbano\s+/i, '')
  const partes = [
    calle || a.pedestrian || a.amenity || resultado.name,
    a.neighbourhood || a.suburb || a.quarter,
    ciudad,
    a.state,
  ].filter(Boolean)
  const sinRepetidos = partes.filter((parte, i) => partes.indexOf(parte) === i)
  const texto = sinRepetidos.length >= 2 ? sinRepetidos.join(', ') : resultado.display_name
  return (texto || '').slice(0, LARGO_MAXIMO_TEXTO)
}

// Busca direcciones en Colombia, con prioridad al Valle de Aburrá.
// Devuelve [{ label, lat, lng }] con lat y lng como número.
export async function buscarDirecciones(texto, signal) {
  const { consulta, placa } = normalizarConsulta(texto)
  const parametros = new URLSearchParams({
    format: 'jsonv2',
    q: consulta,
    countrycodes: 'co',
    limit: '5',
    'accept-language': 'es',
    addressdetails: '1',
    viewbox: VIEWBOX_VALLE_DE_ABURRA,
  })
  const res = await fetch(`${NOMINATIM_URL}/search?${parametros}`, {
    headers: { 'Accept-Language': 'es' },
    signal,
  })
  if (!res.ok) throw new Error('Nominatim no disponible')
  const datos = await res.json()
  return datos
    .map((resultado) => ({
      label: etiquetaCorta(resultado, placa),
      lat: Number(resultado.lat),
      lng: Number(resultado.lon),
    }))
    .filter((r) => r.label && Number.isFinite(r.lat) && Number.isFinite(r.lng))
    // Una misma vía suele venir en varios tramos con la misma etiqueta: se deja el primero.
    .filter((r, i, lista) => lista.findIndex((otro) => otro.label === r.label) === i)
}

// Geocodificación inversa: coordenadas → dirección legible.
export async function geocodificarInverso(lat, lng, signal) {
  const url =
    `${NOMINATIM_URL}/reverse?format=jsonv2` +
    `&lat=${lat}&lon=${lng}&accept-language=es`
  const res = await fetch(url, {
    headers: { 'Accept-Language': 'es' },
    signal,
  })
  if (!res.ok) throw new Error('Nominatim no disponible')
  const data = await res.json()
  return data.display_name || `${lat.toFixed(5)}, ${lng.toFixed(5)}`
}
