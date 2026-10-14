-- =====================================================================
-- Portal Ugartino Digital - Esquema (SQL Server 2019+)
-- Tablas, claves primarias y foráneas, restricciones e índices.
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

-- ---------------------------------------------------------------------
-- TABLAS MAESTRAS
-- ---------------------------------------------------------------------

IF OBJECT_ID(N'dbo.institucion', N'U') IS NULL
CREATE TABLE dbo.institucion (
    id               BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_institucion PRIMARY KEY,
    nombre           NVARCHAR(150) NOT NULL,
    codigo_modular   NVARCHAR(7)   NOT NULL CONSTRAINT uq_institucion_codigo_modular UNIQUE,
    direccion        NVARCHAR(200) NOT NULL,
    color_principal  NVARCHAR(7)   NOT NULL,
    color_secundario NVARCHAR(7)   NOT NULL,
    logo             NVARCHAR(MAX) NULL,
    actualizado_en   DATETIME2(6)  NOT NULL,
    CONSTRAINT ck_institucion_codigo_modular CHECK (LEN(codigo_modular) = 7 AND codigo_modular NOT LIKE N'%[^0-9]%'),
    CONSTRAINT ck_institucion_colores CHECK (color_principal LIKE N'#[0-9a-fA-F][0-9a-fA-F][0-9a-fA-F][0-9a-fA-F][0-9a-fA-F][0-9a-fA-F]'
                                         AND color_secundario LIKE N'#[0-9a-fA-F][0-9a-fA-F][0-9a-fA-F][0-9a-fA-F][0-9a-fA-F][0-9a-fA-F]')
);
GO

IF OBJECT_ID(N'dbo.nivel_educativo', N'U') IS NULL
CREATE TABLE dbo.nivel_educativo (
    id     BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_nivel_educativo PRIMARY KEY,
    nombre NVARCHAR(30) NOT NULL CONSTRAINT uq_nivel_nombre UNIQUE,
    orden  INT          NOT NULL CONSTRAINT ck_nivel_orden CHECK (orden > 0)
);
GO

IF OBJECT_ID(N'dbo.grado', N'U') IS NULL
CREATE TABLE dbo.grado (
    id       BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_grado PRIMARY KEY,
    nivel_id BIGINT       NOT NULL CONSTRAINT fk_grado_nivel REFERENCES dbo.nivel_educativo (id),
    codigo   NVARCHAR(20) NOT NULL,      -- valor del frontend: "3 años", "1".."6", "1".."5"
    etiqueta NVARCHAR(40) NOT NULL,      -- "1ro grado", "3ro año", "4 años"
    orden    INT          NOT NULL CONSTRAINT ck_grado_orden CHECK (orden > 0),
    CONSTRAINT uq_grado_nivel_codigo UNIQUE (nivel_id, codigo)
);
GO

IF OBJECT_ID(N'dbo.seccion', N'U') IS NULL
CREATE TABLE dbo.seccion (
    id       BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_seccion PRIMARY KEY,
    grado_id BIGINT       NOT NULL CONSTRAINT fk_seccion_grado REFERENCES dbo.grado (id),
    letra    NVARCHAR(2)  NOT NULL,
    turno    NVARCHAR(10) NOT NULL CONSTRAINT ck_seccion_turno CHECK (turno IN (N'Mañana', N'Tarde')),
    CONSTRAINT uq_seccion_grado_letra UNIQUE (grado_id, letra)
);
GO

IF OBJECT_ID(N'dbo.curso', N'U') IS NULL
CREATE TABLE dbo.curso (
    id          BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_curso PRIMARY KEY,
    nombre      NVARCHAR(120) NOT NULL CONSTRAINT uq_curso_nombre UNIQUE,
    descripcion NVARCHAR(255) NULL
);
GO

IF OBJECT_ID(N'dbo.fase_proyecto', N'U') IS NULL
CREATE TABLE dbo.fase_proyecto (
    id        BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_fase_proyecto PRIMARY KEY,
    orden     INT           NOT NULL CONSTRAINT uq_fase_orden UNIQUE,
    semana    NVARCHAR(10)  NOT NULL,
    titulo    NVARCHAR(80)  NOT NULL,
    actividad NVARCHAR(300) NOT NULL,
    producto  NVARCHAR(150) NOT NULL,
    estado    NVARCHAR(12)  NOT NULL CONSTRAINT ck_fase_estado CHECK (estado IN (N'Completada', N'En curso', N'Pendiente'))
);
GO

-- Personal del colegio: eliminación lógica (estado I) y restauración (estado A).
IF OBJECT_ID(N'dbo.personal', N'U') IS NULL
CREATE TABLE dbo.personal (
    id            BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_personal PRIMARY KEY,
    nombres       NVARCHAR(80)  NOT NULL,
    apellidos     NVARCHAR(80)  NOT NULL,
    dni           NVARCHAR(8)   NOT NULL CONSTRAINT uq_personal_dni UNIQUE,
    cargo         NVARCHAR(20)  NOT NULL CONSTRAINT ck_personal_cargo
                      CHECK (cargo IN (N'Docente', N'Auxiliar', N'Administrativo', N'Directivo', N'Servicio')),
    area          NVARCHAR(80)  NULL,
    celular       NVARCHAR(9)   NULL,
    correo        NVARCHAR(120) NULL,
    sexo          NVARCHAR(1)   NOT NULL CONSTRAINT ck_personal_sexo CHECK (sexo IN (N'M', N'F')),
    fecha_ingreso DATE          NULL,
    estado        NVARCHAR(1)   NOT NULL CONSTRAINT ck_personal_estado CHECK (estado IN (N'A', N'I')),
    created_at    DATETIME2(6)  NOT NULL,
    updated_at    DATETIME2(6)  NULL,
    deleted_at    DATETIME2(6)  NULL,
    restored_at   DATETIME2(6)  NULL,
    CONSTRAINT ck_personal_dni CHECK (LEN(dni) = 8 AND dni NOT LIKE N'%[^0-9]%'),
    CONSTRAINT ck_personal_celular CHECK (celular IS NULL OR (LEN(celular) = 9 AND celular LIKE N'9%' AND celular NOT LIKE N'%[^0-9]%'))
);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'ix_personal_estado')
    CREATE INDEX ix_personal_estado ON dbo.personal (estado);
GO

-- ---------------------------------------------------------------------
-- PERSONAS Y CUENTAS
-- ---------------------------------------------------------------------

IF OBJECT_ID(N'dbo.usuario', N'U') IS NULL
CREATE TABLE dbo.usuario (
    id              BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_usuario PRIMARY KEY,
    nombre          NVARCHAR(120) NOT NULL,
    correo          NVARCHAR(120) NOT NULL CONSTRAINT uq_usuario_correo UNIQUE,
    contrasena_hash NVARCHAR(200) NOT NULL,   -- PBKDF2-HMAC-SHA256 con sal
    perfil          NVARCHAR(15)  NOT NULL CONSTRAINT ck_usuario_perfil CHECK (perfil IN (N'administrador', N'docente', N'alumno')),
    estado          NVARCHAR(10)  NOT NULL CONSTRAINT ck_usuario_estado CHECK (estado IN (N'Activo', N'Inactivo')),
    creado          DATE          NOT NULL,
    ultimo_acceso   DATETIME2(6)  NULL
);
GO

IF OBJECT_ID(N'dbo.docente', N'U') IS NULL
CREATE TABLE dbo.docente (
    id              BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_docente PRIMARY KEY,
    usuario_id      BIGINT        NULL CONSTRAINT fk_docente_usuario REFERENCES dbo.usuario (id),
    nombres         NVARCHAR(80)  NOT NULL,
    apellidos       NVARCHAR(80)  NOT NULL,
    dni             NVARCHAR(8)   NOT NULL CONSTRAINT uq_docente_dni UNIQUE,
    telefono        NVARCHAR(20)  NULL,
    especialidad    NVARCHAR(120) NULL,
    colegiatura     NVARCHAR(40)  NULL,
    codigo_plaza    NVARCHAR(30)  NULL,
    condicion       NVARCHAR(30)  NULL,
    nivel_educativo NVARCHAR(30)  NULL,
    fecha_ingreso   DATE          NULL,
    CONSTRAINT ck_docente_dni CHECK (LEN(dni) = 8 AND dni NOT LIKE N'%[^0-9]%')
);
GO
-- Una cuenta se vincula a lo sumo a un docente (un UNIQUE común admitiría un solo NULL).
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'uq_docente_usuario')
    CREATE UNIQUE INDEX uq_docente_usuario ON dbo.docente (usuario_id) WHERE usuario_id IS NOT NULL;
GO

IF OBJECT_ID(N'dbo.estudiante', N'U') IS NULL
CREATE TABLE dbo.estudiante (
    id         BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_estudiante PRIMARY KEY,
    usuario_id BIGINT        NULL CONSTRAINT fk_estudiante_usuario REFERENCES dbo.usuario (id),
    nombres    NVARCHAR(80)  NOT NULL,
    apellidos  NVARCHAR(80)  NOT NULL,
    dni        NVARCHAR(8)   NOT NULL CONSTRAINT uq_estudiante_dni UNIQUE,
    genero     NVARCHAR(1)   NULL CONSTRAINT ck_estudiante_genero CHECK (genero IN (N'M', N'F')),
    telefono   NVARCHAR(20)  NULL,
    correo     NVARCHAR(120) NULL,
    creado     DATE          NOT NULL,
    CONSTRAINT ck_estudiante_dni CHECK (LEN(dni) = 8 AND dni NOT LIKE N'%[^0-9]%')
);
GO
-- Migración para bases creadas antes de agregar el género.
IF COL_LENGTH(N'dbo.estudiante', N'genero') IS NULL
    EXEC (N'ALTER TABLE dbo.estudiante ADD genero NVARCHAR(1) NULL CONSTRAINT ck_estudiante_genero CHECK (genero IN (N''M'', N''F''))');
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'uq_estudiante_usuario')
    CREATE UNIQUE INDEX uq_estudiante_usuario ON dbo.estudiante (usuario_id) WHERE usuario_id IS NOT NULL;
GO

-- ---------------------------------------------------------------------
-- CURSOS POR SECCIÓN Y HORARIO
-- ---------------------------------------------------------------------

IF OBJECT_ID(N'dbo.asignacion', N'U') IS NULL
CREATE TABLE dbo.asignacion (
    id         BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_asignacion PRIMARY KEY,
    codigo     NVARCHAR(20) NOT NULL CONSTRAINT uq_asignacion_codigo UNIQUE,
    curso_id   BIGINT       NOT NULL CONSTRAINT fk_asignacion_curso REFERENCES dbo.curso (id),
    seccion_id BIGINT       NOT NULL CONSTRAINT fk_asignacion_seccion REFERENCES dbo.seccion (id),
    docente_id BIGINT       NOT NULL CONSTRAINT fk_asignacion_docente REFERENCES dbo.docente (id),
    aula       NVARCHAR(60) NULL,
    CONSTRAINT uq_asignacion_curso_seccion UNIQUE (curso_id, seccion_id)
);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'ix_asignacion_docente')
    CREATE INDEX ix_asignacion_docente ON dbo.asignacion (docente_id);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'ix_asignacion_seccion')
    CREATE INDEX ix_asignacion_seccion ON dbo.asignacion (seccion_id);
GO

IF OBJECT_ID(N'dbo.horario', N'U') IS NULL
CREATE TABLE dbo.horario (
    id            BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_horario PRIMARY KEY,
    asignacion_id BIGINT       NOT NULL CONSTRAINT fk_horario_asignacion REFERENCES dbo.asignacion (id) ON DELETE CASCADE,
    dia           NVARCHAR(10) NOT NULL CONSTRAINT ck_horario_dia CHECK (dia IN (N'Lunes', N'Martes', N'Miércoles', N'Jueves', N'Viernes')),
    hora_inicio   TIME         NOT NULL,
    hora_fin      TIME         NOT NULL,
    CONSTRAINT ck_horario_rango CHECK (hora_fin > hora_inicio),
    CONSTRAINT uq_horario_bloque UNIQUE (asignacion_id, dia, hora_inicio)
);
GO

-- ---------------------------------------------------------------------
-- MATRÍCULA
-- ---------------------------------------------------------------------

IF OBJECT_ID(N'dbo.matricula', N'U') IS NULL
CREATE TABLE dbo.matricula (
    id              BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_matricula PRIMARY KEY,
    estudiante_id   BIGINT       NOT NULL CONSTRAINT fk_matricula_estudiante REFERENCES dbo.estudiante (id),
    seccion_id      BIGINT       NOT NULL CONSTRAINT fk_matricula_seccion REFERENCES dbo.seccion (id),
    anio_academico  INT          NOT NULL CONSTRAINT ck_matricula_anio CHECK (anio_academico BETWEEN 2000 AND 2100),
    fecha_matricula DATE         NOT NULL,
    estado          NVARCHAR(10) NOT NULL CONSTRAINT ck_matricula_estado CHECK (estado IN (N'Activa', N'Retirada')),
    fecha_retiro    DATE         NULL,
    CONSTRAINT uq_matricula_estudiante_anio UNIQUE (estudiante_id, anio_academico),
    CONSTRAINT ck_matricula_retiro CHECK ((estado = N'Retirada') OR fecha_retiro IS NULL)
);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'ix_matricula_seccion_estado')
    CREATE INDEX ix_matricula_seccion_estado ON dbo.matricula (seccion_id, estado);
GO

-- ---------------------------------------------------------------------
-- ASISTENCIA
-- ---------------------------------------------------------------------

-- Matriz diaria por aula (P/T/F/J). "pendiente" = sin fila.
IF OBJECT_ID(N'dbo.asistencia_diaria', N'U') IS NULL
CREATE TABLE dbo.asistencia_diaria (
    id             BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_asistencia_diaria PRIMARY KEY,
    estudiante_id  BIGINT       NOT NULL CONSTRAINT fk_asistencia_estudiante REFERENCES dbo.estudiante (id),
    fecha          DATE         NOT NULL,
    estado         NVARCHAR(12) NOT NULL CONSTRAINT ck_asistencia_estado CHECK (estado IN (N'presente', N'tardanza', N'falta', N'justificado')),
    registrado_por BIGINT       NULL CONSTRAINT fk_asistencia_usuario REFERENCES dbo.usuario (id),
    actualizado_en DATETIME2(6) NOT NULL,
    CONSTRAINT uq_asistencia_estudiante_fecha UNIQUE (estudiante_id, fecha)
);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'ix_asistencia_fecha_estado')
    CREATE INDEX ix_asistencia_fecha_estado ON dbo.asistencia_diaria (fecha, estado) INCLUDE (estudiante_id);
GO

-- Asistencia por curso: una sesión por curso y fecha.
IF OBJECT_ID(N'dbo.sesion_clase', N'U') IS NULL
CREATE TABLE dbo.sesion_clase (
    id             BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_sesion_clase PRIMARY KEY,
    asignacion_id  BIGINT       NOT NULL CONSTRAINT fk_sesion_asignacion REFERENCES dbo.asignacion (id),
    fecha          DATE         NOT NULL,
    registrado_por BIGINT       NULL CONSTRAINT fk_sesion_usuario REFERENCES dbo.usuario (id),
    actualizado_en DATETIME2(6) NOT NULL,
    CONSTRAINT uq_sesion_asignacion_fecha UNIQUE (asignacion_id, fecha)
);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'ix_sesion_fecha')
    CREATE INDEX ix_sesion_fecha ON dbo.sesion_clase (fecha);
GO

IF OBJECT_ID(N'dbo.asistencia_curso', N'U') IS NULL
CREATE TABLE dbo.asistencia_curso (
    id            BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_asistencia_curso PRIMARY KEY,
    sesion_id     BIGINT        NOT NULL CONSTRAINT fk_asistencia_curso_sesion REFERENCES dbo.sesion_clase (id) ON DELETE CASCADE,
    estudiante_id BIGINT        NOT NULL CONSTRAINT fk_asistencia_curso_estudiante REFERENCES dbo.estudiante (id),
    estado        NVARCHAR(12)  NOT NULL CONSTRAINT ck_asistencia_curso_estado CHECK (estado IN (N'asistio', N'tardanza', N'falta', N'justificado')),
    observacion   NVARCHAR(255) NULL,
    CONSTRAINT uq_asistencia_curso_sesion_estudiante UNIQUE (sesion_id, estudiante_id)
);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'ix_asistencia_curso_estudiante')
    CREATE INDEX ix_asistencia_curso_estudiante ON dbo.asistencia_curso (estudiante_id);
GO

-- ---------------------------------------------------------------------
-- JUSTIFICACIONES, AVISOS Y COMUNICADOS
-- ---------------------------------------------------------------------

IF OBJECT_ID(N'dbo.justificacion', N'U') IS NULL
CREATE TABLE dbo.justificacion (
    id              BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_justificacion PRIMARY KEY,
    estudiante_id   BIGINT        NOT NULL CONSTRAINT fk_justificacion_estudiante REFERENCES dbo.estudiante (id),
    asignacion_id   BIGINT        NULL CONSTRAINT fk_justificacion_asignacion REFERENCES dbo.asignacion (id),
    fecha           DATE          NOT NULL,
    motivo          NVARCHAR(200) NOT NULL,
    detalle         NVARCHAR(500) NULL,
    archivo_nombre  NVARCHAR(255) NULL,
    estado          NVARCHAR(10)  NOT NULL CONSTRAINT ck_justificacion_estado CHECK (estado IN (N'Pendiente', N'Aprobada', N'Observada')),
    estado_previo   NVARCHAR(12)  NULL CONSTRAINT ck_justificacion_estado_previo
                        CHECK (estado_previo IN (N'presente', N'tardanza', N'falta', N'justificado', N'pendiente')),
    observacion     NVARCHAR(500) NULL,
    fecha_solicitud DATETIME2(6)  NOT NULL,
    revisado_por    BIGINT        NULL CONSTRAINT fk_justificacion_revisor REFERENCES dbo.usuario (id),
    fecha_revision  DATETIME2(6)  NULL,
    CONSTRAINT ck_justificacion_revision CHECK ((estado = N'Pendiente' AND fecha_revision IS NULL) OR (estado <> N'Pendiente' AND fecha_revision IS NOT NULL))
);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'ix_justificacion_estudiante_fecha')
    CREATE INDEX ix_justificacion_estudiante_fecha ON dbo.justificacion (estudiante_id, fecha);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'ix_justificacion_estado')
    CREATE INDEX ix_justificacion_estado ON dbo.justificacion (estado);
-- Una sola solicitud pendiente por estudiante, fecha y curso (o día completo).
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'uq_justificacion_pendiente')
    CREATE UNIQUE INDEX uq_justificacion_pendiente ON dbo.justificacion (estudiante_id, fecha, asignacion_id) WHERE estado = N'Pendiente';
GO

IF OBJECT_ID(N'dbo.aviso_familia', N'U') IS NULL
CREATE TABLE dbo.aviso_familia (
    id            BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_aviso_familia PRIMARY KEY,
    estudiante_id BIGINT       NOT NULL CONSTRAINT fk_aviso_estudiante REFERENCES dbo.estudiante (id),
    enviado_en    DATETIME2(6) NOT NULL,
    faltas        INT          NOT NULL CONSTRAINT ck_aviso_faltas CHECK (faltas >= 0),
    enviado_por   BIGINT       NULL CONSTRAINT fk_aviso_usuario REFERENCES dbo.usuario (id)
);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'ix_aviso_estudiante_fecha')
    CREATE INDEX ix_aviso_estudiante_fecha ON dbo.aviso_familia (estudiante_id, enviado_en);
GO

IF OBJECT_ID(N'dbo.comunicado', N'U') IS NULL
CREATE TABLE dbo.comunicado (
    id             BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_comunicado PRIMARY KEY,
    titulo         NVARCHAR(150)  NOT NULL,
    contenido      NVARCHAR(2000) NOT NULL,
    prioridad      NVARCHAR(10)   NOT NULL CONSTRAINT ck_comunicado_prioridad CHECK (prioridad IN (N'Alta', N'Media', N'Baja')),
    asignacion_id  BIGINT         NULL CONSTRAINT fk_comunicado_asignacion REFERENCES dbo.asignacion (id),
    autor_id       BIGINT         NULL CONSTRAINT fk_comunicado_autor REFERENCES dbo.usuario (id),
    fecha_creacion DATE           NOT NULL
);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'ix_comunicado_asignacion')
    CREATE INDEX ix_comunicado_asignacion ON dbo.comunicado (asignacion_id);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'ix_comunicado_fecha')
    CREATE INDEX ix_comunicado_fecha ON dbo.comunicado (fecha_creacion);
GO
