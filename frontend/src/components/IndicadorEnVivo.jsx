// Indicador pequeño del estado de los avisos en tiempo real.
// Gris: el navegador no está conectado. Amarillo (sinEventos): conectado, pero el
// servicio que envía los avisos no los está recibiendo. Verde: todo en vivo.
function IndicadorEnVivo({ estado, sinEventos = false }) {
  const enVivo = estado === 'conectado'
  const { clase, texto } = !enVivo
    ? { clase: 'estado-gris', texto: 'Reconectando…' }
    : sinEventos
      ? { clase: 'estado-ambar', texto: 'Sin eventos' }
      : { clase: 'estado-verde', texto: 'En vivo' }

  return (
    <span className={`status-chip ${clase}`} role="status">
      ● {texto}
    </span>
  )
}

export default IndicadorEnVivo
