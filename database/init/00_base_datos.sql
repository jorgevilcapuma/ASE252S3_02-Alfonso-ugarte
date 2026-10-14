-- =====================================================================
-- Portal Ugartino Digital (I.E. 20191 Alfonso Ugarte) - Base de datos
-- Crea la base si no existe. Ejecutar antes de 01_esquema.sql.
-- =====================================================================
IF DB_ID(N'ugartino_db') IS NULL
BEGIN
    CREATE DATABASE ugartino_db COLLATE Modern_Spanish_CI_AS;
END
GO
