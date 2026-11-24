import { crearCliente } from './sesionHttp'

// Cliente dedicado al microservicio ms-perfil (puerto 8082).
// Sin VITE_PERFIL_URL (desarrollo local) se llama a http://localhost:8082.
// En el servidor se compila como cadena vacía: las peticiones salen a /api/perfiles
// en el mismo origen y nginx las reenvía al microservicio. Por eso se usa ?? y
// no ||, que convertiría la cadena vacía otra vez en localhost.
// La sesión va en la cookie HttpOnly (ver sesionHttp.js), no en un header Authorization.
const perfilClient = crearCliente(import.meta.env.VITE_PERFIL_URL ?? 'http://localhost:8082')

export default perfilClient
