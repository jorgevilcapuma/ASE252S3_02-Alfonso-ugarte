-- =====================================================================
-- Elimina las tablas del modelo anterior (institucion, matricula, sesion_clase, ...).
-- NO se ejecuta automáticamente: corre este script a mano solo si tu base ugartino_db
-- se creó con la versión anterior y ya no necesitas esos datos.
--   sqlcmd -C -S localhost,1434 -U sa -P Admin12345 -i database/migracion/eliminar_tablas_anteriores.sql
-- Alternativa con Docker: "docker compose down -v" borra el volumen y la base se recrea vacía.
-- =====================================================================
USE ugartino_db;
GO

-- La asistencia_personal anterior apuntaba a la tabla personal (ahora apunta a docentes)
-- y referencia a usuario, así que se elimina primero.
IF OBJECT_ID(N'dbo.asistencia_personal', N'U') IS NOT NULL AND COL_LENGTH(N'dbo.asistencia_personal', N'personal_id') IS NOT NULL
    DROP TABLE dbo.asistencia_personal;
GO

-- Ordenadas de dependientes a referenciadas.
DROP TABLE IF EXISTS dbo.asistencia_curso;
DROP TABLE IF EXISTS dbo.sesion_clase;
DROP TABLE IF EXISTS dbo.asistencia_diaria;
DROP TABLE IF EXISTS dbo.justificacion;
DROP TABLE IF EXISTS dbo.aviso_familia;
DROP TABLE IF EXISTS dbo.comunicado;
DROP TABLE IF EXISTS dbo.horario;
DROP TABLE IF EXISTS dbo.matricula;
DROP TABLE IF EXISTS dbo.asignacion;
DROP TABLE IF EXISTS dbo.curso;
DROP TABLE IF EXISTS dbo.seccion;
DROP TABLE IF EXISTS dbo.grado;
DROP TABLE IF EXISTS dbo.nivel_educativo;
DROP TABLE IF EXISTS dbo.estudiante;
DROP TABLE IF EXISTS dbo.docente;
DROP TABLE IF EXISTS dbo.usuario;
DROP TABLE IF EXISTS dbo.fase_proyecto;
DROP TABLE IF EXISTS dbo.institucion;
GO

DROP TABLE IF EXISTS dbo.personal;
GO
-- Después ejecuta database/init/01_esquema.sql para crear las tablas nuevas.
