import { crearCliente } from './sesionHttp'

// Cliente dedicado al microservicio ms-admin-reportes (puerto 8084).
// Sin VITE_ADMIN_URL (desarrollo local) se llama a http://localhost:8084.
// En el servidor se compila como cadena vacía: las peticiones salen a /api/admin
// en el mismo origen y nginx las reenvía al microservicio. Por eso se usa ?? y
// no ||, que convertiría la cadena vacía otra vez en localhost.
// La sesión va en la cookie HttpOnly (ver sesionHttp.js), no en un header Authorization.
const adminClient = crearCliente(import.meta.env.VITE_ADMIN_URL ?? 'http://localhost:8084')

export default adminClient
