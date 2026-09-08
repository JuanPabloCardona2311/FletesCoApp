-- Script base para SCRUM-62 en Supabase/PostgreSQL.
-- Hibernate puede crear estas tablas con spring.jpa.hibernate.ddl-auto=update;
-- este script deja la estructura minima reproducible para el equipo.

CREATE TABLE IF NOT EXISTS conductores (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT UNIQUE REFERENCES usuarios(id),
    calificacion_promedio NUMERIC(3, 2),
    cancelaciones_totales INTEGER NOT NULL DEFAULT 0,
    ubicacion_lat NUMERIC(10, 7),
    ubicacion_lng NUMERIC(10, 7)
);

CREATE TABLE IF NOT EXISTS despachadores (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT UNIQUE REFERENCES usuarios(id),
    nombre_empresa VARCHAR(150),
    nit VARCHAR(20)
);

CREATE TABLE IF NOT EXISTS vehiculos (
    id BIGSERIAL PRIMARY KEY,
    conductor_id BIGINT NOT NULL REFERENCES conductores(id),
    tipo_vehiculo VARCHAR(50) NOT NULL,
    placa VARCHAR(10) NOT NULL,
    capacidad_carga NUMERIC(10, 2) NOT NULL,
    estado_verificacion VARCHAR(10) NOT NULL DEFAULT 'PENDIENTE',
    activo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX IF NOT EXISTS idx_vehiculos_conductor_id ON vehiculos(conductor_id);
CREATE INDEX IF NOT EXISTS idx_despachadores_usuario_id ON despachadores(usuario_id);
CREATE INDEX IF NOT EXISTS idx_conductores_usuario_id ON conductores(usuario_id);
