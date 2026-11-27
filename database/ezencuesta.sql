-- =====================================================================
--  ezEncuestaFX - Script de base de datos (PostgreSQL)
--
--  1) Crear la base (una sola vez, conectado a "postgres"):
--        CREATE DATABASE ezencuesta;
--  2) Conectarse a la base ezencuesta y ejecutar este script completo.
--  3) Revisar usuario/contraseña en
--        src/main/resources/com/tuempresa/ezencuestafx/db.properties
-- =====================================================================

-- Usuarios del sistema (docentes y estudiantes de investigación)
CREATE TABLE IF NOT EXISTS usuarios (
    id               SERIAL PRIMARY KEY,
    nombre_completo  VARCHAR(120) NOT NULL,
    correo           VARCHAR(120) NOT NULL UNIQUE,
    usuario          VARCHAR(40)  NOT NULL UNIQUE,
    contrasena_hash  VARCHAR(200) NOT NULL,
    rol              VARCHAR(30)  NOT NULL
                     CHECK (rol IN ('DOCENTE', 'ESTUDIANTE_INVESTIGACION')),
    creado_en        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Banco de preguntas: cada pregunta pertenece a un usuario y puede
-- reutilizarse en todas las encuestas que ese usuario cree.
CREATE TABLE IF NOT EXISTS preguntas (
    id           SERIAL PRIMARY KEY,
    usuario_id   INT          NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    enunciado    TEXT         NOT NULL,
    tipo         VARCHAR(30)  NOT NULL
                 CHECK (tipo IN ('TEXTO', 'NUMERICA', 'SELECCION_UNICA', 'SELECCION_MULTIPLE')),
    obligatoria  BOOLEAN      NOT NULL DEFAULT FALSE,
    creada_en    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_preguntas_usuario ON preguntas(usuario_id);

-- Opciones de las preguntas de selección única / múltiple
CREATE TABLE IF NOT EXISTS opciones_pregunta (
    id           SERIAL PRIMARY KEY,
    pregunta_id  INT          NOT NULL REFERENCES preguntas(id) ON DELETE CASCADE,
    texto        VARCHAR(200) NOT NULL,
    orden        INT          NOT NULL
);

-- Encuestas guardadas por cada usuario
CREATE TABLE IF NOT EXISTS encuestas (
    id              SERIAL PRIMARY KEY,
    usuario_id      INT          NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    titulo          VARCHAR(200) NOT NULL,
    creada_en       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizada_en  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Qué preguntas usa cada encuesta y en qué orden.
-- Una misma pregunta puede aparecer en muchas encuestas (reutilización).
CREATE TABLE IF NOT EXISTS encuesta_preguntas (
    encuesta_id  INT     NOT NULL REFERENCES encuestas(id) ON DELETE CASCADE,
    pregunta_id  INT     NOT NULL REFERENCES preguntas(id) ON DELETE RESTRICT,
    orden        INT     NOT NULL,
    obligatoria  BOOLEAN NOT NULL,
    PRIMARY KEY (encuesta_id, pregunta_id)
);
