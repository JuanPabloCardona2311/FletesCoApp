# FletesCo

Plataforma web que conecta despachadores de carga con conductores independientes en Colombia.

> **Nota importante sobre la base de datos:**  
> El proyecto utiliza **PostgreSQL compartido en la nube mediante Supabase**. Todo el equipo trabaja con la misma base de datos, esquemas y tablas sincronizadas en tiempo real, sin necesidad de instalar motores de base de datos locales ni contenedores de base de datos.

---

## 📁 Estructura del proyecto

```text
FletesCoApp/
├── ms-auth/             Microservicio de autenticación        (puerto 8081)
├── ms-perfil/           Microservicio de perfiles              (puerto 8082)
├── ms-solicitudes/      Microservicio de solicitudes de flete  (puerto 8083)
├── ms-admin-reportes/   Microservicio de administración        (puerto 8084)
├── frontend/            Aplicación web en React + Vite         (puerto 5173 con Docker)
├── docs/                Documentación del proyecto
├── backend/             Monolito antiguo (puerto 8080), en desuso: se retirará
└── docker-compose.yml   Levanta los 4 microservicios y el frontend
```

### Microservicios

| Servicio | Puerto | Responsabilidad |
|---|---|---|
| `ms-auth` | 8081 | Registro e inicio de sesión de usuarios (`/api/auth`) |
| `ms-perfil` | 8082 | Perfil del conductor (vehículos y ubicación) y del despachador (`/api/perfiles`) |
| `ms-solicitudes` | 8083 | Publicar, aceptar y seguir solicitudes de flete hasta la confirmación de entrega (`/api/solicitudes`) |
| `ms-admin-reportes` | 8084 | Administración: usuarios, disputas y reportes (`/api/admin`) |

---

## ⚙️ Configuración inicial

1. **Clonar el repositorio:**
   ```bash
   git clone https://github.com/JuanPabloCardona2311/FletesCoApp.git
   cd FletesCoApp
   ```

2. **Crear el archivo de entorno `.env` en la raíz del proyecto:**
   Desde la raíz del repositorio (la carpeta `FletesCoApp/`), copia la plantilla `.env.example` a un nuevo archivo llamado `.env`:
   ```bash
   cp .env.example .env
   ```
   *(En Windows PowerShell: `Copy-Item .env.example .env`)*.

3. **Configurar las credenciales:**
   Abre el archivo `.env` de la raíz y solicita a tu equipo los valores reales de cada variable:
   ```env
   SUPABASE_DB_URL=AQUI_VA_LA_URL_DE_LA_BASE_DE_DATOS
   SUPABASE_DB_USERNAME=AQUI_VA_EL_USUARIO_DE_LA_BASE_DE_DATOS
   SUPABASE_DB_PASSWORD=AQUI_VA_LA_CONTRASEÑA_DEL_EQUIPO
   JWT_SECRET=AQUI_VA_EL_SECRETO_COMPARTIDO_DEL_EQUIPO
   ```
   > **`JWT_SECRET`** debe ser el **mismo valor para todo el equipo** (no inventes el tuyo: si cada quien usa uno distinto, los tokens no son válidos entre servicios). Pídelo por el canal privado del equipo.

   > **El archivo `.env` nunca se sube a GitHub** (ya está en el `.gitignore`). Comparte sus valores solo por el canal privado del equipo.

---

## 🐳 Correr con Docker (forma principal)

**Requisito:** tener instalado y abierto **Docker Desktop**.

Todos los comandos se ejecutan desde la raíz del repositorio:

```bash
docker compose up --build          # primer arranque: construye las imágenes y levanta todo
docker compose up -d               # arranques siguientes, en segundo plano
docker compose ps                  # ver el estado de los contenedores
docker compose logs -f <servicio>  # ver los logs en vivo (ej: docker compose logs -f ms-auth)
docker compose down                # detener y eliminar los contenedores
```

Cuando todo esté arriba, abre la aplicación en:
```text
http://localhost:5173
```

> **Antes de levantar:** libera los puertos **8081, 8082, 8083, 8084 y 5173**. Detén los servicios que tengas corriendo desde el Spring Boot Dashboard, el `npm run dev` del frontend y cualquier contenedor que hayas levantado a mano (puedes verlos con `docker ps`).

> **Los contenedores tienen `restart: unless-stopped`:** en segundo plano (`-d`) siguen corriendo aunque cierres la terminal, y vuelven a arrancar solos cada vez que se reinicia Docker Desktop o el computador. Para apagarlos de verdad usa `docker compose down`.

---

## 💻 Correr desde VS Code (alternativa sin Docker)

1. **Microservicios:**
   * Abre la carpeta raíz del proyecto (`FletesCoApp/`) en **VS Code**.
   * Abre el panel del **Spring Boot Dashboard** e inicia los microservicios que necesites (`ms-auth`, `ms-perfil`, `ms-solicitudes`, `ms-admin-reportes`).
   * También puedes usar la pestaña **Ejecutar y depurar** (`Ctrl + Shift + D`): hay una configuración por microservicio y una llamada **"Todos los microservicios"** que los levanta juntos.
   * El archivo `.vscode/launch.json` carga automáticamente las variables de entorno del archivo `.env` de la raíz.

2. **Frontend** (en otra terminal):
   ```bash
   cd frontend
   npm install
   npm run dev
   ```
   La aplicación queda disponible en `http://localhost:5173`.
