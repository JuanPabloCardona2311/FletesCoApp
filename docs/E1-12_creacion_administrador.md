# E1-12: Procedimiento Documentado para Crear la Primera Cuenta Administrador

## 1. Contexto y Justificación
Por motivos de seguridad (**E1-11**), el rol `ADMINISTRADOR` no puede ser auto-asignado a través del formulario de registro público de FleteCo. Para habilitar la primera cuenta de administrador (o administradores del sistema), se utiliza una inserción controlada directamente en la base de datos (Supabase / PostgreSQL) garantizando el cifrado de contraseña con BCrypt y el estado activo.

---

## 2. Requisitos Previos
1. Acceso al proyecto de FleteCo en la consola de **Supabase** (o cualquier cliente PostgreSQL conectado a la base de datos).
2. Abrir la pestaña **SQL Editor** en el panel lateral izquierdo de Supabase.

---

## 3. Script SQL de Creación en Supabase

Reemplaza `CORREO_DEL_ADMIN`, `CLAVE_SEGURA`, `TELEFONO_DEL_ADMIN` y `DOCUMENTO_DEL_ADMIN` por los valores reales solo en el **SQL Editor**. No guardes esos valores en este documento ni en el repositorio.

Si el correo ya existe, el script deja la cuenta activa como administrador y no cambia su contraseña.

Ejecuta el siguiente script en el **SQL Editor** de Supabase:

```sql
-- 1. Habilitar la extensión de cifrado pgcrypto (incluida por defecto en Supabase)
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- 2. Insertar el usuario administrador
INSERT INTO usuarios (
    nombre,
    email,
    password_hash,
    telefono,
    tipo_documento_identidad,
    numero_documento_identidad,
    tipo_usuario,
    fecha_registro,
    estado
) VALUES (
    'Administrador FleteCo',
    'CORREO_DEL_ADMIN',
    crypt('CLAVE_SEGURA', gen_salt('bf', 10)), -- Cifra la contraseña con BCrypt (costo 10)
    'TELEFONO_DEL_ADMIN',
    'CC',
    'DOCUMENTO_DEL_ADMIN',
    'ADMINISTRADOR',
    NOW(),
    'ACTIVO'
)
ON CONFLICT (email) DO UPDATE SET
    estado = 'ACTIVO',
    tipo_usuario = 'ADMINISTRADOR';

-- 3. Verificar que el usuario fue creado y está activo
SELECT id, nombre, email, tipo_usuario, estado, fecha_registro 
FROM usuarios 
WHERE email = 'CORREO_DEL_ADMIN';
```

---

## 4. Puntos Clave de la Inserción
* **¿Cómo queda activo?** El campo `estado` se establece explícitamente en `'ACTIVO'` (valor del enum `Usuario.EstadoUsuario.ACTIVO`).
* **¿Cómo se cifra la contraseña?** La función `crypt('tu_clave', gen_salt('bf', 10))` genera un hash compatible con el `BCryptPasswordEncoder` de Spring Security, permitiendo que el administrador pueda iniciar sesión inmediatamente por el endpoint `/api/auth/login`.
* **Tablas asociadas:** Los administradores **no** requieren filas en las tablas `conductores` ni `despachadores`.
