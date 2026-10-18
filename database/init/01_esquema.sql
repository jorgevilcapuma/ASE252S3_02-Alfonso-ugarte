-- =====================================================================
-- Portal Ugartino Digital - Esquema (SQL Server 2019+)
-- Control de asistencia de estudiantes, docentes y personal administrativo.
--
-- Maestras:        usuarios, grados_secciones, docentes, estudiantes, cursos
-- Transaccionales: asistencia_estudiante, asistencia_personal, justificaciones
--
-- Idempotente y sin errores de validación estática (IntelliSense/SSMS).
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

-- 1. Cuentas de acceso al portal.
IF OBJECT_ID(N'dbo.usuarios', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.usuarios (
        id              BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_usuarios PRIMARY KEY,
        nombre          NVARCHAR(120) NOT NULL,
        correo          NVARCHAR(120) NOT NULL CONSTRAINT uq_usuarios_correo UNIQUE,
        contrasena_hash NVARCHAR(200) NOT NULL,
        perfil          NVARCHAR(15)  NOT NULL CONSTRAINT ck_usuarios_perfil CHECK (perfil IN (N'administrador', N'docente', N'alumno')),
        estado          NVARCHAR(10)  NOT NULL CONSTRAINT ck_usuarios_estado CHECK (estado IN (N'Activo', N'Inactivo')),
        creado          DATE          NOT NULL,
        ultimo_acceso   DATETIME2(6)  NULL
    );
END;
GO

-- 2. Aulas: nivel + grado + sección (letra) con su turno.
IF OBJECT_ID(N'dbo.grados_secciones', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.grados_secciones (
        id      BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_grados_secciones PRIMARY KEY,
        nivel   NVARCHAR(12) NOT NULL CONSTRAINT ck_grados_secciones_nivel CHECK (nivel IN (N'Inicial', N'Primaria', N'Secundaria')),
        grado   NVARCHAR(10) NOT NULL,
        seccion NVARCHAR(2)  NOT NULL,
        turno   NVARCHAR(10) NOT NULL CONSTRAINT ck_grados_secciones_turno CHECK (turno IN (N'Mañana', N'Tarde')),
        CONSTRAINT uq_grados_secciones UNIQUE (nivel, grado, seccion)
    );
END;
GO

-- 3. Docentes y personal del colegio
IF OBJECT_ID(N'dbo.docentes', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.docentes (
        id             BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_docentes PRIMARY KEY,
        usuario_id     BIGINT        NULL CONSTRAINT fk_docentes_usuario REFERENCES dbo.usuarios (id),
        nombres        NVARCHAR(80)  NOT NULL,
        apellidos      NVARCHAR(80)  NOT NULL,
        dni            NVARCHAR(8)   NOT NULL CONSTRAINT uq_docentes_dni UNIQUE,
        cargo          NVARCHAR(20)  NOT NULL CONSTRAINT ck_docentes_cargo
                           CHECK (cargo IN (N'Docente', N'Auxiliar', N'Administrativo', N'Directivo', N'Servicio')),
        especialidad   NVARCHAR(120) NULL,
        area           NVARCHAR(80)  NULL,
        celular        NVARCHAR(9)   NULL,
        correo         NVARCHAR(120) NULL,
        sexo           NVARCHAR(1)   NOT NULL CONSTRAINT ck_docentes_sexo CHECK (sexo IN (N'M', N'F')),
        fecha_ingreso  DATE          NULL,
        estado         NVARCHAR(1)   NOT NULL CONSTRAINT ck_docentes_estado CHECK (estado IN (N'A', N'I')),
        creado_en      DATETIME2(6)  NOT NULL,
        actualizado_en DATETIME2(6)  NULL,
        CONSTRAINT ck_docentes_dni CHECK (LEN(dni) = 8 AND dni NOT LIKE N'%[^0-9]%'),
        CONSTRAINT ck_docentes_celular CHECK (celular IS NULL OR (LEN(celular) = 9 AND celular LIKE N'9%' AND celular NOT LIKE N'%[^0-9]%'))
    );
END;
GO

-- Índices para dbo.docentes (SQL Dinámico para evitar falsos errores de compilación)
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'uq_docentes_usuario' AND object_id = OBJECT_ID(N'dbo.docentes'))
    EXEC(N'CREATE UNIQUE INDEX uq_docentes_usuario ON dbo.docentes (usuario_id) WHERE usuario_id IS NOT NULL;');

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'ix_docentes_estado' AND object_id = OBJECT_ID(N'dbo.docentes'))
    EXEC(N'CREATE INDEX ix_docentes_estado ON dbo.docentes (estado);');
GO

-- 4. Estudiantes
IF OBJECT_ID(N'dbo.estudiantes', N'U') IS NULL
BEGIN
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
END;
GO

-- Índices para dbo.estudiantes
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'uq_estudiantes_usuario' AND object_id = OBJECT_ID(N'dbo.estudiantes'))
    EXEC(N'CREATE UNIQUE INDEX uq_estudiantes_usuario ON dbo.estudiantes (usuario_id) WHERE usuario_id IS NOT NULL;');

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'ix_estudiantes_grado_seccion' AND object_id = OBJECT_ID(N'dbo.estudiantes'))
    EXEC(N'CREATE INDEX ix_estudiantes_grado_seccion ON dbo.estudiantes (grado_seccion_id, estado);');
GO

-- 5. Cursos
IF OBJECT_ID(N'dbo.cursos', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.cursos (
        id               BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_cursos PRIMARY KEY,
        nombre           NVARCHAR(120) NOT NULL,
        grado_seccion_id BIGINT        NOT NULL CONSTRAINT fk_cursos_grado_seccion REFERENCES dbo.grados_secciones (id),
        docente_id       BIGINT        NOT NULL CONSTRAINT fk_cursos_docente REFERENCES dbo.docentes (id),
        CONSTRAINT uq_cursos_nombre_aula UNIQUE (nombre, grado_seccion_id)
    );
END;
GO

-- Índices para dbo.cursos
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'ix_cursos_docente' AND object_id = OBJECT_ID(N'dbo.cursos'))
    EXEC(N'CREATE INDEX ix_cursos_docente ON dbo.cursos (docente_id);');
GO

-- ---------------------------------------------------------------------
-- TABLAS TRANSACCIONALES
-- ---------------------------------------------------------------------

-- 6. Asistencia de estudiantes
IF OBJECT_ID(N'dbo.asistencia_estudiante', N'U') IS NULL
BEGIN
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
END;
GO

-- Índices para dbo.asistencia_estudiante
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'uq_asistencia_estudiante_curso' AND object_id = OBJECT_ID(N'dbo.asistencia_estudiante'))
    EXEC(N'CREATE UNIQUE INDEX uq_asistencia_estudiante_curso ON dbo.asistencia_estudiante (estudiante_id, curso_id, fecha) WHERE curso_id IS NOT NULL;');

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'uq_asistencia_estudiante_diaria' AND object_id = OBJECT_ID(N'dbo.asistencia_estudiante'))
    EXEC(N'CREATE UNIQUE INDEX uq_asistencia_estudiante_diaria ON dbo.asistencia_estudiante (estudiante_id, fecha) WHERE curso_id IS NULL;');

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'ix_asistencia_estudiante_fecha' AND object_id = OBJECT_ID(N'dbo.asistencia_estudiante'))
    EXEC(N'CREATE INDEX ix_asistencia_estudiante_fecha ON dbo.asistencia_estudiante (fecha, estado) INCLUDE (estudiante_id, curso_id);');
GO

-- 7. Asistencia del personal
IF OBJECT_ID(N'dbo.asistencia_personal', N'U') IS NULL
BEGIN
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
END;
GO

-- Índices para dbo.asistencia_personal
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'ix_asistencia_personal_fecha' AND object_id = OBJECT_ID(N'dbo.asistencia_personal'))
    EXEC(N'CREATE INDEX ix_asistencia_personal_fecha ON dbo.asistencia_personal (fecha);');
GO

-- 8. Justificaciones
IF OBJECT_ID(N'dbo.justificaciones', N'U') IS NULL
BEGIN
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
END;
GO

-- Índices para dbo.justificaciones
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'ix_justificaciones_estudiante_fecha' AND object_id = OBJECT_ID(N'dbo.justificaciones'))
    EXEC(N'CREATE INDEX ix_justificaciones_estudiante_fecha ON dbo.justificaciones (estudiante_id, fecha);');

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'ix_justificaciones_estado' AND object_id = OBJECT_ID(N'dbo.justificaciones'))
    EXEC(N'CREATE INDEX ix_justificaciones_estado ON dbo.justificaciones (estado);');

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'uq_justificaciones_pendiente' AND object_id = OBJECT_ID(N'dbo.justificaciones'))
    EXEC(N'CREATE UNIQUE INDEX uq_justificaciones_pendiente ON dbo.justificaciones (estudiante_id, fecha, curso_id) WHERE estado = N''Pendiente'';');
GO

-- ---------------------------------------------------------------------
-- CONSULTAS DE COMPROBACIÓN
-- ---------------------------------------------------------------------

SELECT * FROM dbo.usuarios;
SELECT * FROM dbo.grados_secciones;
SELECT * FROM dbo.docentes;
SELECT * FROM dbo.estudiantes;
SELECT * FROM dbo.cursos;
SELECT * FROM dbo.asistencia_estudiante;
SELECT * FROM dbo.asistencia_personal;
SELECT * FROM dbo.justificaciones;
GO