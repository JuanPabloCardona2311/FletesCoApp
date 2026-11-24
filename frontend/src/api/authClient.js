import { crearCliente } from './sesionHttp'

// Cliente dedicado al microservicio ms-auth (puerto 8081).
// Sin VITE_AUTH_URL (desarrollo local) se llama a http://localhost:8081.
// En el servidor se compila como cadena vacía: las peticiones salen a /api/auth
// en el mismo origen y nginx las reenvía al microservicio. Por eso se usa ?? y
// no ||, que convertiría la cadena vacía otra vez en localhost.
// La sesión va en la cookie HttpOnly (ver sesionHttp.js), no en un header Authorization.
const authClient = crearCliente(import.meta.env.VITE_AUTH_URL ?? 'http://localhost:8081')

export default authClient
