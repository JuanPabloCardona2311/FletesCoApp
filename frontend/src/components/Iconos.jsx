// Íconos de línea de la interfaz (decorativos: el texto vecino da el significado).
function Icono({ children, tamano = 15 }) {
  return (
    <svg
      className="icono"
      width={tamano}
      height={tamano}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={2}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      focusable="false"
    >
      {children}
    </svg>
  )
}

export function IconoEditar() {
  return (
    <Icono tamano={16}>
      <path d="M12 20h9" />
      <path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4Z" />
    </Icono>
  )
}

export function IconoMas() {
  return (
    <Icono tamano={16}>
      <path d="M12 5v14M5 12h14" />
    </Icono>
  )
}

export function IconoVolver() {
  return (
    <Icono tamano={16}>
      <path d="M19 12H5M12 19l-7-7 7-7" />
    </Icono>
  )
}

export function IconoMapa() {
  return (
    <Icono>
      <path d="m3 6 6-3 6 3 6-3v15l-6 3-6-3-6 3Z" />
      <path d="M9 3v15M15 6v15" />
    </Icono>
  )
}

export function IconoAlerta() {
  return (
    <Icono>
      <path d="M10.3 3.9 1.8 18a2 2 0 0 0 1.7 3h17a2 2 0 0 0 1.7-3L13.7 3.9a2 2 0 0 0-3.4 0Z" />
      <path d="M12 9v4M12 17h.01" />
    </Icono>
  )
}

export function IconoTelefono() {
  return (
    <Icono tamano={14}>
      <path d="M22 16.9v3a2 2 0 0 1-2.2 2 19.8 19.8 0 0 1-8.6-3.1 19.5 19.5 0 0 1-6-6A19.8 19.8 0 0 1 2.1 4.2 2 2 0 0 1 4.1 2h3a2 2 0 0 1 2 1.7c.1.9.4 1.8.7 2.7a2 2 0 0 1-.5 2.1L8 9.8a16 16 0 0 0 6 6l1.3-1.3a2 2 0 0 1 2.1-.4c.9.3 1.8.6 2.7.7a2 2 0 0 1 1.7 2Z" />
    </Icono>
  )
}

export function IconoUbicacion() {
  return (
    <Icono>
      <path d="M20 10c0 6-8 12-8 12S4 16 4 10a8 8 0 0 1 16 0Z" />
      <circle cx="12" cy="10" r="3" />
    </Icono>
  )
}

export function IconoCalendario() {
  return (
    <Icono tamano={14}>
      <rect x="3" y="4" width="18" height="18" rx="2" />
      <path d="M16 2v4M8 2v4M3 10h18" />
    </Icono>
  )
}

export function IconoBandera() {
  return (
    <Icono tamano={14}>
      <path d="M4 22V4M4 15s1-1 4-1 5 2 8 2 4-1 4-1V3s-1 1-4 1-5-2-8-2-4 1-4 1" />
    </Icono>
  )
}

export function IconoCaja() {
  return (
    <Icono tamano={14}>
      <path d="M21 8 12 3 3 8v8l9 5 9-5V8Z" />
      <path d="m3 8 9 5 9-5M12 13v8" />
    </Icono>
  )
}

export function IconoPeso() {
  return (
    <Icono tamano={14}>
      <path d="M12 3v18M7 21h10M5 7h14" />
      <path d="m5 7-3 7a3 3 0 0 0 6 0Z" />
      <path d="m19 7-3 7a3 3 0 0 0 6 0Z" />
    </Icono>
  )
}

export function IconoCamion() {
  return (
    <Icono tamano={14}>
      <path d="M1 5h13v11H1zM14 9h4l4 4v3h-8" />
      <circle cx="6" cy="18" r="2" />
      <circle cx="18" cy="18" r="2" />
    </Icono>
  )
}

export function IconoAncla() {
  return (
    <Icono tamano={14}>
      <circle cx="12" cy="5" r="3" />
      <path d="M12 8v14M5 12H2a10 10 0 0 0 20 0h-3" />
    </Icono>
  )
}

export function IconoReloj() {
  return (
    <Icono>
      <circle cx="12" cy="12" r="10" />
      <path d="M12 6v6l4 2" />
    </Icono>
  )
}
