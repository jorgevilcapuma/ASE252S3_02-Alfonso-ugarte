-- =====================================================================
-- Portal Ugartino Digital - Esquema (SQL Server 2019+)
-- Control de asistencia de estudiantes, docentes y personal administrativo.
--
--   Maestras:       usuarios, grados_secciones, docentes, estudiantes, cursos
--   Transaccionales: asistencia_estudiante, asistencia_personal, justificaciones
--
-- Las entidades JPA se validan contra este esquema (spring.jpa.hibernate.ddl-auto=validate).
-- Los datos maestros y de demostración los carga la aplicación al iniciar (DatosIniciales.java).
-- Idempotente: solo crea lo que no existe.
-- =====================================================================
USE ugartino_db;
GO
-- Requeridos por los índices filtrados (sqlcmd usa QUOTED_IDENTIFIER OFF por defecto).
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO
-- Sin caché de IDENTITY: si el servidor se reinicia, los ids no saltan de 1000 en 1000 (1, 2, 3... y no 10002).
-- Para corregir saltos ya existentes: database/mantenimiento/renumerar_ids.sql
ALTER DATABASE SCOPED CONFIGURATION SET IDENTITY_CACHE = OFF;
GO

-- ---------------------------------------------------------------------
-- TABLAS MAESTRAS
-- ---------------------------------------------------------------------

-- Cuentas de acceso al portal.
IF OBJECT_ID(N'dbo.usuarios', N'U') IS NULL
CREATE TABLE dbo.usuarios (
    id              BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_usuarios PRIMARY KEY,
    nombre          NVARCHAR(120) NOT NULL,
    correo          NVARCHAR(120) NOT NULL CONSTRAINT uq_usuarios_correo UNIQUE,
    contrasena_hash NVARCHAR(200) NOT NULL,   -- PBKDF2-HMAC-SHA256 con sal
    perfil          NVARCHAR(15)  NOT NULL CONSTRAINT ck_usuarios_perfil CHECK (perfil IN (N'administrador', N'docente', N'alumno')),
    estado          NVARCHAR(10)  NOT NULL CONSTRAINT ck_usuarios_estado CHECK (estado IN (N'Activo', N'Inactivo')),
    creado          DATE          NOT NULL,
    ultimo_acceso   DATETIME2(6)  NULL
);
GO

-- Aulas: nivel + grado + sección (letra) con su turno.
IF OBJECT_ID(N'dbo.grados_secciones', N'U') IS NULL
CREATE TABLE dbo.grados_secciones (
    id      BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_grados_secciones PRIMARY KEY,
    nivel   NVARCHAR(12) NOT NULL CONSTRAINT ck_grados_secciones_nivel CHECK (nivel IN (N'Inicial', N'Primaria', N'Secundaria')),
    grado   NVARCHAR(10) NOT NULL,      -- "3 años".."5 años" en Inicial, "1".."6" en Primaria, "1".."5" en Secundaria
    seccion NVARCHAR(2)  NOT NULL,
    turno   NVARCHAR(10) NOT NULL CONSTRAINT ck_grados_secciones_turno CHECK (turno IN (N'Mañana', N'Tarde')),
    estado  NVARCHAR(1)  NOT NULL CONSTRAINT df_grados_secciones_estado DEFAULT (N'A')
                CONSTRAINT ck_grados_secciones_estado CHECK (estado IN (N'A', N'I')),
    CONSTRAINT uq_grados_secciones UNIQUE (nivel, grado, seccion)
);
GO
-- Bases creadas antes de la eliminación lógica de aulas.
IF COL_LENGTH(N'dbo.grados_secciones', N'estado') IS NULL
    ALTER TABLE dbo.grados_secciones ADD estado NVARCHAR(1) NOT NULL CONSTRAINT df_grados_secciones_estado DEFAULT (N'A')
        CONSTRAINT ck_grados_secciones_estado CHECK (estado IN (N'A', N'I'));
GO

-- Todo el personal del colegio (docentes, auxiliares, administrativos, directivos y de servicio).
-- Eliminación lógica (estado I) y restauración (estado A).
IF OBJECT_ID(N'dbo.docentes', N'U') IS NULL
CREATE TABLE dbo.docentes (
    id            BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_docentes PRIMARY KEY,
    usuario_id    BIGINT        NULL CONSTRAINT fk_docentes_usuario REFERENCES dbo.usuarios (id),
    nombres       NVARCHAR(80)  NOT NULL,
    apellidos     NVARCHAR(80)  NOT NULL,
    dni           NVARCHAR(8)   NOT NULL CONSTRAINT uq_docentes_dni UNIQUE,
    cargo         NVARCHAR(20)  NOT NULL CONSTRAINT ck_docentes_cargo
                      CHECK (cargo IN (N'Docente', N'Auxiliar', N'Administrativo', N'Directivo', N'Servicio')),
    especialidad  NVARCHAR(120) NULL,
    area          NVARCHAR(80)  NULL,
    celular       NVARCHAR(9)   NULL,
    correo        NVARCHAR(120) NULL,
    sexo          NVARCHAR(1)   NOT NULL CONSTRAINT ck_docentes_sexo CHECK (sexo IN (N'M', N'F')),
    fecha_ingreso DATE          NULL,
    estado        NVARCHAR(1)   NOT NULL CONSTRAINT ck_docentes_estado CHECK (estado IN (N'A', N'I')),
    creado_en     DATETIME2(6)  NOT NULL,
    actualizado_en DATETIME2(6) NULL,
    CONSTRAINT ck_docentes_dni CHECK (LEN(dni) = 8 AND dni NOT LIKE N'%[^0-9]%'),
    CONSTRAINT ck_docentes_celular CHECK (celular IS NULL OR (LEN(celular) = 9 AND celular LIKE N'9%' AND celular NOT LIKE N'%[^0-9]%'))
);
GO
-- Una cuenta se vincula a lo sumo a un docente (un UNIQUE común admitiría un solo NULL).
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'uq_docentes_usuario')
    CREATE UNIQUE INDEX uq_docentes_usuario ON dbo.docentes (usuario_id) WHERE usuario_id IS NOT NULL;
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'ix_docentes_estado')
    CREATE INDEX ix_docentes_estado ON dbo.docentes (estado);
GO

-- Estudiantes con su aula actual. Eliminación lógica (estado I = retirado) y restauración (estado A).
IF OBJECT_ID(N'dbo.estudiantes', N'U') IS NULL
CREATE TABLE dbo.estudiantes (
    id               BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_estudiantes PRIMARY KEY,
    usuario_id       BIGINT        NULL CONSTRAINT fk_estudiantes_usuario REFERENCES dbo.usuarios (id),
    grado_seccion_id BIGINT        NOT NULL CONSTRAINT fk_estudiantes_grado_seccion REFERENCES dbo.grados_secciones (id),
    nombres          NVARCHAR(80)  NOT NULL,
    apellidos        NVARCHAR(80)  NOT NULL,
    dni              NVARCHAR(8)   NOT NULL CONSTRAINT uq_estudiantes_dni UNIQUE,
    genero           NVARCHAR(1)   NULL CONSTRAINT ck_estudiantes_genero CHECK (genero IN (N'M', N'F')),
    telefono         NVARCHAR(20)  NULL,
    correo           NVARCHAR(120) NULL,
    estado           NVARCHAR(1)   NOT NULL CONSTRAINT ck_estudiantes_estado CHECK (estado IN (N'A', N'I')),
    creado           DATE          NOT NULL,
    CONSTRAINT ck_estudiantes_dni CHECK (LEN(dni) = 8 AND dni NOT LIKE N'%[^0-9]%')
);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'uq_estudiantes_usuario')
    CREATE UNIQUE INDEX uq_estudiantes_usuario ON dbo.estudiantes (usuario_id) WHERE usuario_id IS NOT NULL;
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'ix_estudiantes_grado_seccion')
    CREATE INDEX ix_estudiantes_grado_seccion ON dbo.estudiantes (grado_seccion_id, estado);
GO

-- Curso dictado en un aula por un docente (p. ej. "Matemática - 1ro grado A").
IF OBJECT_ID(N'dbo.cursos', N'U') IS NULL
CREATE TABLE dbo.cursos (
    id               BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_cursos PRIMARY KEY,
    nombre           NVARCHAR(120) NOT NULL,
    grado_seccion_id BIGINT        NOT NULL CONSTRAINT fk_cursos_grado_seccion REFERENCES dbo.grados_secciones (id),
    docente_id       BIGINT        NOT NULL CONSTRAINT fk_cursos_docente REFERENCES dbo.docentes (id),
    estado           NVARCHAR(1)   NOT NULL CONSTRAINT df_cursos_estado DEFAULT (N'A')
                         CONSTRAINT ck_cursos_estado CHECK (estado IN (N'A', N'I')),
    CONSTRAINT uq_cursos_nombre_aula UNIQUE (nombre, grado_seccion_id)
);
GO
-- Bases creadas antes de la eliminación lógica de cursos.
IF COL_LENGTH(N'dbo.cursos', N'estado') IS NULL
    ALTER TABLE dbo.cursos ADD estado NVARCHAR(1) NOT NULL CONSTRAINT df_cursos_estado DEFAULT (N'A')
        CONSTRAINT ck_cursos_estado CHECK (estado IN (N'A', N'I'));
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'ix_cursos_docente')
    CREATE INDEX ix_cursos_docente ON dbo.cursos (docente_id);
GO

-- ---------------------------------------------------------------------
-- TABLAS TRANSACCIONALES
-- ---------------------------------------------------------------------

-- Asistencia de estudiantes: curso_id NULL = asistencia diaria del aula; con curso = asistencia de esa clase.
-- "pendiente" significa que no hay fila.
IF OBJECT_ID(N'dbo.asistencia_estudiante', N'U') IS NULL
CREATE TABLE dbo.asistencia_estudiante (
    id             BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_asistencia_estudiante PRIMARY KEY,
    estudiante_id  BIGINT        NOT NULL CONSTRAINT fk_asistencia_estudiante_estudiante REFERENCES dbo.estudiantes (id),
    curso_id       BIGINT        NULL CONSTRAINT fk_asistencia_estudiante_curso REFERENCES dbo.cursos (id),
    fecha          DATE          NOT NULL,
    estado         NVARCHAR(12)  NOT NULL CONSTRAINT ck_asistencia_estudiante_estado
                       CHECK (estado IN (N'presente', N'tardanza', N'falta', N'justificado')),
    observacion    NVARCHAR(255) NULL,
    registrado_por BIGINT        NULL CONSTRAINT fk_asistencia_estudiante_usuario REFERENCES dbo.usuarios (id),
    actualizado_en DATETIME2(6)  NOT NULL
);
GO
-- Un registro por estudiante, fecha y curso; y uno solo diario (sin curso).
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'uq_asistencia_estudiante_curso')
    CREATE UNIQUE INDEX uq_asistencia_estudiante_curso ON dbo.asistencia_estudiante (estudiante_id, curso_id, fecha) WHERE curso_id IS NOT NULL;
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'uq_asistencia_estudiante_diaria')
    CREATE UNIQUE INDEX uq_asistencia_estudiante_diaria ON dbo.asistencia_estudiante (estudiante_id, fecha) WHERE curso_id IS NULL;
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'ix_asistencia_estudiante_fecha')
    CREATE INDEX ix_asistencia_estudiante_fecha ON dbo.asistencia_estudiante (fecha, estado) INCLUDE (estudiante_id, curso_id);
GO

-- Marcación de entrada y salida del personal (tabla docentes): una por persona y fecha.
IF OBJECT_ID(N'dbo.asistencia_personal', N'U') IS NULL
CREATE TABLE dbo.asistencia_personal (
    id             BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_asistencia_personal PRIMARY KEY,
    docente_id     BIGINT        NOT NULL CONSTRAINT fk_asistencia_personal_docente REFERENCES dbo.docentes (id),
    fecha          DATE          NOT NULL,
    hora_entrada   TIME          NOT NULL,
    hora_salida    TIME          NULL,
    estado         NVARCHAR(10)  NOT NULL CONSTRAINT ck_asistencia_personal_estado CHECK (estado IN (N'puntual', N'tardanza')),
    observacion    NVARCHAR(200) NULL,
    registrado_por BIGINT        NULL CONSTRAINT fk_asistencia_personal_usuario REFERENCES dbo.usuarios (id),
    actualizado_en DATETIME2(6)  NOT NULL,
    CONSTRAINT uq_asistencia_personal_fecha UNIQUE (docente_id, fecha),
    CONSTRAINT ck_asistencia_personal_salida CHECK (hora_salida IS NULL OR hora_salida > hora_entrada)
);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'ix_asistencia_personal_fecha')
    CREATE INDEX ix_asistencia_personal_fecha ON dbo.asistencia_personal (fecha);
GO

-- Solicitudes de justificación de inasistencias de estudiantes (curso_id NULL = día completo).
IF OBJECT_ID(N'dbo.justificaciones', N'U') IS NULL
CREATE TABLE dbo.justificaciones (
    id              BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_justificaciones PRIMARY KEY,
    estudiante_id   BIGINT        NOT NULL CONSTRAINT fk_justificaciones_estudiante REFERENCES dbo.estudiantes (id),
    curso_id        BIGINT        NULL CONSTRAINT fk_justificaciones_curso REFERENCES dbo.cursos (id),
    fecha           DATE          NOT NULL,
    motivo          NVARCHAR(200) NOT NULL,
    detalle         NVARCHAR(500) NULL,
    archivo_nombre  NVARCHAR(255) NULL,
    estado          NVARCHAR(10)  NOT NULL CONSTRAINT ck_justificaciones_estado CHECK (estado IN (N'Pendiente', N'Aprobada', N'Observada')),
    estado_previo   NVARCHAR(12)  NULL CONSTRAINT ck_justificaciones_estado_previo
                        CHECK (estado_previo IN (N'presente', N'tardanza', N'falta', N'justificado', N'pendiente')),
    observacion     NVARCHAR(500) NULL,
    fecha_solicitud DATETIME2(6)  NOT NULL,
    revisado_por    BIGINT        NULL CONSTRAINT fk_justificaciones_revisor REFERENCES dbo.usuarios (id),
    fecha_revision  DATETIME2(6)  NULL,
    CONSTRAINT ck_justificaciones_revision CHECK ((estado = N'Pendiente' AND fecha_revision IS NULL) OR (estado <> N'Pendiente' AND fecha_revision IS NOT NULL))
);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'ix_justificaciones_estudiante_fecha')
    CREATE INDEX ix_justificaciones_estudiante_fecha ON dbo.justificaciones (estudiante_id, fecha);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'ix_justificaciones_estado')
    CREATE INDEX ix_justificaciones_estado ON dbo.justificaciones (estado);
-- Una sola solicitud pendiente por estudiante, fecha y curso (o día completo).
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'uq_justificaciones_pendiente')
    CREATE UNIQUE INDEX uq_justificaciones_pendiente ON dbo.justificaciones (estudiante_id, fecha, curso_id) WHERE estado = N'Pendiente';
GO
