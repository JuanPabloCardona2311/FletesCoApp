import { useCallback, useEffect, useState } from 'react'
import perfilClient from '../api/perfilClient'
import './DocumentosConductor.css'

// E2-02 / E2-03. Subir documentos es OPCIONAL: nada en la plataforma se bloquea por ellos.
const TARJETAS = [
  { tipo: 'LICENCIA_CONDUCCION', titulo: 'Licencia de conducción', deVehiculo: false },
  { tipo: 'SOAT', titulo: 'SOAT', deVehiculo: true },
  { tipo: 'TECNOMECANICA', titulo: 'Tecnomecánica', deVehiculo: true },
]

const ESTADOS = {
  PENDIENTE: { texto: 'Pendiente', clase: 'pendiente' },
  VERIFICADO: { texto: 'Verificado', clase: 'verificado' },
  RECHAZADO: { texto: 'Rechazado', clase: 'rechazado' },
}

const TAMANO_MAXIMO = 5 * 1024 * 1024
const EXTENSIONES = '.pdf,.jpg,.jpeg,.png,application/pdf,image/jpeg,image/png'

const manana = () => {
  const fecha = new Date()
  fecha.setDate(fecha.getDate() + 1)
  return fecha.toLocaleDateString('en-CA') // AAAA-MM-DD en hora local
}

const formatearFecha = (iso) =>
  iso ? new Date(`${iso}T00:00:00`).toLocaleDateString('es-CO', { dateStyle: 'medium' }) : 'Sin fecha'

const mensajeDeError = (requestError, porDefecto) => {
  const data = requestError.response?.data
  if (data?.errores) return Object.values(data.errores)[0]
  if (data?.error) return data.error
  if (!requestError.response) return 'No fue posible conectar con el servidor.'
  return porDefecto
}

function DocumentosConductor({ vehiculoActivo }) {
  const [documentos, setDocumentos] = useState([])
  const [cargandoLista, setCargandoLista] = useState(true)
  const [errorLista, setErrorLista] = useState('')
  const [formularios, setFormularios] = useState({})
  const [subiendo, setSubiendo] = useState(null)
  const [mensajes, setMensajes] = useState({})

  const cargarDocumentos = useCallback(async () => {
    setErrorLista('')
    try {
      const response = await perfilClient.get('/api/perfiles/conductor/documentos')
      setDocumentos(response.data || [])
    } catch (requestError) {
      setErrorLista(mensajeDeError(requestError, 'No fue posible cargar tus documentos.'))
    } finally {
      setCargandoLista(false)
    }
  }, [])

  useEffect(() => {
    cargarDocumentos()
  }, [cargarDocumentos])

  const documentoDe = (tarjeta) => documentos.find((doc) =>
    doc.tipoDocumento === tarjeta.tipo
    && (!tarjeta.deVehiculo || doc.vehiculoId === vehiculoActivo?.id))

  const actualizarFormulario = (tipo, cambios) =>
    setFormularios((previos) => ({ ...previos, [tipo]: { ...previos[tipo], ...cambios } }))

  const ponerMensaje = (tipo, mensaje) => setMensajes((previos) => ({ ...previos, [tipo]: mensaje }))

  const handleSubir = async (event, tarjeta) => {
    event.preventDefault()
    const { archivo, fecha } = formularios[tarjeta.tipo] || {}
    ponerMensaje(tarjeta.tipo, null)

    if (!archivo) {
      ponerMensaje(tarjeta.tipo, { error: 'Selecciona un archivo PDF, JPG o PNG.' })
      return
    }
    if (archivo.size > TAMANO_MAXIMO) {
      ponerMensaje(tarjeta.tipo, { error: 'El archivo supera el máximo permitido de 5 MB.' })
      return
    }
    if (!fecha) {
      ponerMensaje(tarjeta.tipo, { error: 'Indica la fecha de vencimiento.' })
      return
    }

    const datos = new FormData()
    datos.append('archivo', archivo)
    datos.append('tipoDocumento', tarjeta.tipo)
    datos.append('fechaVencimiento', fecha)
    if (tarjeta.deVehiculo) datos.append('vehiculoId', vehiculoActivo.id)

    setSubiendo(tarjeta.tipo)
    try {
      await perfilClient.post('/api/perfiles/conductor/documentos', datos)
      ponerMensaje(tarjeta.tipo, { exito: 'Documento subido. Queda pendiente de verificación.' })
      actualizarFormulario(tarjeta.tipo, { archivo: null, fecha: '', claveInput: Date.now() })
      await cargarDocumentos()
    } catch (requestError) {
      ponerMensaje(tarjeta.tipo, { error: mensajeDeError(requestError, 'No fue posible subir el documento.') })
    } finally {
      setSubiendo(null)
    }
  }

  // Los enlaces firmados duran pocos minutos: si ya venció, se piden nuevos antes de abrirlo.
  const handleVer = async (event, documento) => {
    if (new Date(documento.enlaceExpiraEn) > new Date()) return
    event.preventDefault()
    await cargarDocumentos()
    ponerMensaje(documento.tipoDocumento, { exito: 'El enlace se renovó. Vuelve a hacer clic en "Ver archivo".' })
  }

  return (
    <section className="profile-panel documentos-panel">
      <div className="panel-heading">
        <div>
          <h2>Mis documentos</h2>
          <p className="solicitudes-subtitulo">
            Opcional por ahora. Formatos PDF, JPG o PNG de máximo 5 MB.
          </p>
        </div>
        <button type="button" className="btn-actualizar" onClick={cargarDocumentos} disabled={cargandoLista}>
          ↻ Actualizar
        </button>
      </div>

      {errorLista && <p className="error" role="alert">{errorLista}</p>}

      <div className="documentos-grid">
        {TARJETAS.map((tarjeta) => {
          const documento = documentoDe(tarjeta)
          const estado = documento ? ESTADOS[documento.estadoVerificacion] : null
          const sinVehiculo = tarjeta.deVehiculo && !vehiculoActivo
          const formulario = formularios[tarjeta.tipo] || {}
          const mensaje = mensajes[tarjeta.tipo]

          return (
            <article key={tarjeta.tipo} className="documento-card">
              <div className="documento-card-heading">
                <div>
                  <h3>{tarjeta.titulo}</h3>
                  <small>
                    {tarjeta.deVehiculo
                      ? (vehiculoActivo ? `Vehículo ${vehiculoActivo.placa}` : 'Sin vehículo activo')
                      : 'Del conductor'}
                  </small>
                </div>
                {estado ? (
                  <span className={`documento-estado documento-estado-${estado.clase}`}>{estado.texto}</span>
                ) : (
                  <span className="documento-estado documento-estado-sin-subir">Sin subir</span>
                )}
              </div>

              {documento && (
                <div className="documento-detalle">
                  <span>
                    Vence: <strong>{formatearFecha(documento.fechaVencimiento)}</strong>
                    {documento.vencido && <span className="documento-vencido"> · Vencido</span>}
                  </span>
                  {documento.enlaceVer ? (
                    <a
                      href={documento.enlaceVer}
                      target="_blank"
                      rel="noopener noreferrer"
                      onClick={(event) => handleVer(event, documento)}
                    >
                      Ver archivo
                    </a>
                  ) : (
                    <span className="documento-sin-enlace">Archivo no disponible por ahora</span>
                  )}
                </div>
              )}

              {sinVehiculo ? (
                <p className="documento-nota">Agrega o activa un vehículo para subir este documento.</p>
              ) : (
                <form className="documento-form" onSubmit={(event) => handleSubir(event, tarjeta)}>
                  <label>
                    Fecha de vencimiento
                    <input
                      type="date"
                      min={manana()}
                      value={formulario.fecha || ''}
                      onChange={(event) => actualizarFormulario(tarjeta.tipo, { fecha: event.target.value })}
                      required
                    />
                  </label>
                  <label>
                    Archivo
                    <input
                      key={formulario.claveInput || 'inicial'}
                      type="file"
                      accept={EXTENSIONES}
                      onChange={(event) => actualizarFormulario(tarjeta.tipo, { archivo: event.target.files?.[0] || null })}
                      required
                    />
                  </label>
                  <button type="submit" disabled={subiendo !== null}>
                    {subiendo === tarjeta.tipo ? 'Subiendo...' : documento ? 'Reemplazar' : 'Subir'}
                  </button>
                </form>
              )}

              {mensaje?.error && <p className="error" role="alert">{mensaje.error}</p>}
              {mensaje?.exito && <p className="success" role="status">{mensaje.exito}</p>}
            </article>
          )
        })}
      </div>
    </section>
  )
}

export default DocumentosConductor
