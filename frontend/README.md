# Frontend de FletesCo

Aplicación web desarrollada con React y Vite para conectar despachadores y conductores de la plataforma FletesCo.

## Requisitos

- Node.js instalado.
- Backend de FletesCo ejecutándose en `http://localhost:8080`.

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

La URL del backend se configura en el archivo `.env`:

```env
VITE_API_URL=http://localhost:8080
```

Cuando el backend esté desplegado, reemplaza esa dirección por su URL pública y vuelve a iniciar o construir el frontend.

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
├── api/          Cliente centralizado de Axios.
├── components/   Componentes reutilizables.
├── pages/        Pantallas de la aplicación.
├── App.jsx       Configuración de rutas.
└── main.jsx      Punto de entrada y BrowserRouter.
```
