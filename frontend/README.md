# Frontend de FletesCo

Aplicación web desarrollada con React y Vite para conectar despachadores y conductores de la plataforma FletesCo.

## Requisitos

- Node.js instalado.
- Microservicios de FletesCo ejecutándose en los puertos 8081 a 8084.

## Instalación

Desde esta carpeta, instala las dependencias:

```powershell
npm.cmd install
```

## Ejecutar el frontend

Inicia el servidor de desarrollo con:

```powershell
npm.cmd run dev
```

Después abre la dirección que muestra Vite, normalmente:

```text
http://localhost:5173
```

## Configuración de la API

La URL de cada microservicio se configura en el archivo `.env`:

```env
VITE_AUTH_URL=http://localhost:8081
VITE_PERFIL_URL=http://localhost:8082
VITE_SOLICITUDES_URL=http://localhost:8083
VITE_ADMIN_URL=http://localhost:8084
```

Cuando los microservicios estén desplegados, reemplaza esas direcciones por sus URL públicas y vuelve a iniciar o construir el frontend.

## Rutas actuales

- `/`: inicio de sesión.
- `/registro`: registro de usuarios como conductor o despachador.

## Crear versión de producción

```powershell
npm.cmd run build
```

Para revisar localmente la versión construida:

```powershell
npm.cmd run preview
```

## Estructura principal

```text
src/
├── api/          Un cliente de Axios por microservicio.
├── components/   Componentes reutilizables.
├── pages/        Pantallas de la aplicación.
├── App.jsx       Configuración de rutas.
└── main.jsx      Punto de entrada y BrowserRouter.
```
