-- =====================================================================
-- Portal Ugartino Digital - Corrige los saltos de IDs (1, 10, 10002...)
--
-- SQL Server guarda en caché 1000 valores de IDENTITY y, al reiniciarse (p. ej. el contenedor de Docker), los
-- pierde: el siguiente id salta 1000. Este script:
--   1. Desactiva esa caché (IDENTITY_CACHE = OFF) para que no vuelva a pasar.
--   2. Renumera las cuentas (usuarios) en orden 1, 2, 3... y actualiza todas las tablas que las referencian.
--   3. Ajusta el siguiente id de cada tabla al último usado (para que no salte en el próximo registro).
--
-- Uso (con el backend detenido; las sesiones abiertas deben volver a ingresar):
--   docker stop ugartino-backend
--   docker exec -i ugartino-sqlserver /opt/mssql-tools18/bin/sqlcmd -C -b -I -S localhost -U sa -P <clave> -d ugartino_db < database/mantenimiento/renumerar_ids.sql
--   docker start ugartino-backend
-- Es seguro ejecutarlo varias veces: si no hay saltos, no cambia nada.
-- =====================================================================
USE ugartino_db;
GO
SET NOCOUNT ON;
SET XACT_ABORT ON;
GO

-- 1. Sin caché de IDENTITY: los ids siguen de uno en uno aunque el servidor se reinicie.
ALTER DATABASE SCOPED CONFIGURATION SET IDENTITY_CACHE = OFF;
GO

-- 2. Renumerar usuarios (la única tabla con saltos que referencian otras tablas).
BEGIN TRANSACTION;

SELECT id AS anterior, CAST(ROW_NUMBER() OVER (ORDER BY id) AS BIGINT) AS nuevo
INTO #mapa
FROM dbo.usuarios;

IF EXISTS (SELECT 1 FROM #mapa WHERE anterior <> nuevo)
BEGIN
    PRINT 'Renumerando usuarios:';
    SELECT CONCAT('  ', anterior, ' -> ', nuevo) AS cambio FROM #mapa WHERE anterior <> nuevo;

    -- Las claves foráneas hacia usuarios se suspenden mientras se mueven las filas.
    ALTER TABLE dbo.docentes             NOCHECK CONSTRAINT fk_docentes_usuario;
    ALTER TABLE dbo.estudiantes          NOCHECK CONSTRAINT fk_estudiantes_usuario;
    ALTER TABLE dbo.justificaciones      NOCHECK CONSTRAINT fk_justificaciones_revisor;
    ALTER TABLE dbo.asistencia_estudiante NOCHECK CONSTRAINT fk_asistencia_estudiante_usuario;
    ALTER TABLE dbo.asistencia_personal  NOCHECK CONSTRAINT fk_asistencia_personal_usuario;

    SELECT * INTO #usuarios FROM dbo.usuarios;
    DELETE FROM dbo.usuarios;

    SET IDENTITY_INSERT dbo.usuarios ON;
    INSERT INTO dbo.usuarios (id, nombre, correo, contrasena_hash, perfil, estado, creado, ultimo_acceso)
    SELECT m.nuevo, u.nombre, u.correo, u.contrasena_hash, u.perfil, u.estado, u.creado, u.ultimo_acceso
    FROM #usuarios u JOIN #mapa m ON m.anterior = u.id;
    SET IDENTITY_INSERT dbo.usuarios OFF;

    UPDATE d SET usuario_id = m.nuevo FROM dbo.docentes d JOIN #mapa m ON m.anterior = d.usuario_id;
    UPDATE e SET usuario_id = m.nuevo FROM dbo.estudiantes e JOIN #mapa m ON m.anterior = e.usuario_id;
    UPDATE j SET revisado_por = m.nuevo FROM dbo.justificaciones j JOIN #mapa m ON m.anterior = j.revisado_por;
    UPDATE a SET registrado_por = m.nuevo FROM dbo.asistencia_estudiante a JOIN #mapa m ON m.anterior = a.registrado_por;
    UPDATE a SET registrado_por = m.nuevo FROM dbo.asistencia_personal a JOIN #mapa m ON m.anterior = a.registrado_por;

    -- WITH CHECK vuelve a validar todas las filas: si algo quedara mal, la transacción se revierte.
    ALTER TABLE dbo.docentes             WITH CHECK CHECK CONSTRAINT fk_docentes_usuario;
    ALTER TABLE dbo.estudiantes          WITH CHECK CHECK CONSTRAINT fk_estudiantes_usuario;
    ALTER TABLE dbo.justificaciones      WITH CHECK CHECK CONSTRAINT fk_justificaciones_revisor;
    ALTER TABLE dbo.asistencia_estudiante WITH CHECK CHECK CONSTRAINT fk_asistencia_estudiante_usuario;
    ALTER TABLE dbo.asistencia_personal  WITH CHECK CHECK CONSTRAINT fk_asistencia_personal_usuario;
END
ELSE
    PRINT 'Usuarios: sin saltos, no se renumera.';

COMMIT TRANSACTION;
GO

-- 3. El siguiente id de cada tabla con datos continúa desde el último usado.
DECLARE @tabla SYSNAME, @maximo BIGINT, @sql NVARCHAR(400);
DECLARE tablas CURSOR LOCAL FAST_FORWARD FOR
    SELECT t.name FROM sys.tables t
    WHERE OBJECTPROPERTY(t.object_id, 'TableHasIdentity') = 1 AND t.schema_id = SCHEMA_ID('dbo');
OPEN tablas;
FETCH NEXT FROM tablas INTO @tabla;
WHILE @@FETCH_STATUS = 0
BEGIN
    SET @sql = N'SELECT @m = MAX(id) FROM dbo.' + QUOTENAME(@tabla);
    EXEC sp_executesql @sql, N'@m BIGINT OUTPUT', @m = @maximo OUTPUT;
    -- Solo tablas con filas y con el contador por delante del último id (las vacías no se tocan).
    IF @maximo IS NOT NULL AND IDENT_CURRENT('dbo.' + @tabla) > @maximo
    BEGIN
        PRINT CONCAT(@tabla, ': siguiente id ', @maximo + 1, ' (antes ', IDENT_CURRENT('dbo.' + @tabla) + 1, ')');
        DBCC CHECKIDENT (@tabla, RESEED, @maximo) WITH NO_INFOMSGS;
    END
    FETCH NEXT FROM tablas INTO @tabla;
END
CLOSE tablas;
DEALLOCATE tablas;
GO
