import { createContext, useContext } from 'react'

// Sesión en memoria (ver SesionProvider). Archivo aparte del Provider para que el
// fast refresh de Vite funcione: un archivo de componentes solo exporta componentes.
export const SesionContext = createContext(null)

export function useSesion() {
  return useContext(SesionContext)
}
