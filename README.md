# FleteCoApp

Plataforma web que conecta despachadores de carga con conductores independientes en Colombia.

---

## 🚀 Cómo correr el backend localmente

> **Nota importante sobre la base de datos:**  
> El proyecto ahora utiliza **PostgreSQL compartido en la nube mediante Supabase**, en lugar de una base de datos MySQL local. Esto asegura que todo el equipo trabaje con la misma base de datos, esquemas y tablas sincronizadas en tiempo real sin necesidad de instalar motores de base de datos locales.

### Pasos para iniciar el backend:

1. **Clonar el repositorio:**
   ```bash
   git clone https://github.com/JuanPabloCardona2311/FleteCoApp.git
   cd FleteCoApp
   ```

2. **Crear el archivo de entorno `.env` en la raíz del proyecto:**
   Desde la raíz del repositorio (la carpeta `FleteCoApp/`, no `backend/`), copia la plantilla `.env.example` a un nuevo archivo llamado `.env`:
   ```bash
   cp .env.example .env
   ```
   *(En Windows PowerShell: `Copy-Item .env.example .env`)*.

3. **Configurar las credenciales:**
   Abre el archivo `.env` de la raíz y solicita a tu equipo la contraseña real de la base de datos (`SUPABASE_DB_PASSWORD`) y el secreto compartido (`JWT_SECRET`):
   ```env
   SUPABASE_DB_URL=jdbc:postgresql://aws-0-us-east-2.pooler.supabase.com:5432/postgres?sslmode=require
   SUPABASE_DB_USERNAME=postgres.mjowsnxbjeqnpyormvko
   SUPABASE_DB_PASSWORD=AQUI_VA_LA_CONTRASEÑA_DEL_EQUIPO
   JWT_SECRET=AQUI_VA_EL_SECRETO_COMPARTIDO_DEL_EQUIPO
   ```
   > **`JWT_SECRET`** debe ser el **mismo valor para todo el equipo** (no inventes el tuyo: si cada quien usa uno distinto, los tokens no son válidos entre servicios). Pídelo por el canal privado del equipo y **nunca lo subas a GitHub**.

4. **Correr el proyecto desde VS Code:**
   * Abre la carpeta del proyecto en **VS Code**.
   * Ve a la pestaña **Ejecutar y depurar** (Run and Debug, `Ctrl + Shift + D`) o al panel del **Spring Boot Dashboard**.
   * Inicia la aplicación seleccionando el perfil `Spring Boot-DemoApplication<demo>`.
   * El archivo `.vscode/launch.json` está configurado para cargar automáticamente las variables de entorno de tu archivo `.env`.
   * Una vez iniciado, el backend estará disponible y escuchando peticiones en:
     ```text
     http://localhost:8080
     ```
