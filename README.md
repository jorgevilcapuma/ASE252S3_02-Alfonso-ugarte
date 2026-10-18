# Portal Ugartino Digital — Backend

API REST del control de asistencia de la **I.E. 20191 Alfonso Ugarte**: registra la asistencia de estudiantes
(diaria por aula y por curso), de docentes y del personal administrativo, con justificaciones de inasistencias.
Trabaja sobre 8 tablas en SQL Server, con validaciones, permisos por perfil y reglas de negocio en el servidor.

**Tecnologías** (las mismas del backend del curso): Java 21 · Spring Boot 3.3 · Spring Data JPA (Hibernate 6) ·
Bean Validation · SQL Server 2022 · Lombok · springdoc-openapi (Swagger) · JUnit 5 + MockMvc + H2 para las pruebas.

---

## 1. Ejecutar

### Opción A — Todo con Docker (recomendado)

```bash
docker compose up -d --build
```

Levanta SQL Server (puerto **1434** del host), ejecuta `database/init/*.sql` y arranca el backend en **http://localhost:8080**.
Al iniciar con la base vacía se cargan las tablas maestras y los datos de demostración.

- Swagger: http://localhost:8080/swagger-ui.html (botón **Authorize** con el token del login)
- Detener: `docker compose down` · borrar también los datos: `docker compose down -v`

### Opción B — Backend local sin Docker ni SQL Server (perfil `local`)

```bash
mvn spring-boot:run "-Dspring-boot.run.profiles=local"
```

Usa una base H2 guardada en `./datos-local` (se conserva entre reinicios; bórrala para empezar de cero), crea las tablas
desde las entidades y carga los mismos datos de demostración. Consola de la base: http://localhost:8080/h2-console
(JDBC URL `jdbc:h2:file:./datos-local/ugartino`, usuario `sa`, sin contraseña).

### Opción C — Backend local con un SQL Server propio

```bash
sqlcmd -S localhost -U sa -P <clave> -I -i database/init/00_base_datos.sql
sqlcmd -S localhost -U sa -P <clave> -I -i database/init/01_esquema.sql
mvn spring-boot:run
```

Variables de entorno (todas opcionales):

| Variable | Por defecto | Uso |
|---|---|---|
| `DATABASE_URL` | `jdbc:sqlserver://localhost:1433;databaseName=ugartino_db;...` | Conexión JDBC |
| `DATABASE_USERNAME` / `DATABASE_PASSWORD` | `sa` / `Admin12345` | Credenciales |
| `SERVER_PORT` | `8080` | Puerto HTTP |
| `CORS_ORIGINS` | `http://localhost:4200` | Origen del frontend (`ng serve`) |
| `TOKEN_SECRETO` | (valor de desarrollo) | Firma de los tokens: **cámbialo en producción** |
| `CONTRASENA_INICIAL` | `123456` | Contraseña de las cuentas creadas sin contraseña |
| `DATOS_DEMO` / `DIAS_HISTORIAL` | `false` / `22` | `true` carga los datos de demostración (sección 2) y sus días hábiles de historial |
| `ZONA_HORARIA` | `America/Lima` | Define el "hoy" de la asistencia |

### Pruebas

```bash
mvn test        # 57 pruebas: API completa sobre H2 (modo SQL Server) con reloj fijo
```

### Postman

`postman/Ugartino-Backend-CRUD.postman_collection.json`: CRUD de las 5 tablas maestras (Docentes, Estudiantes, Usuarios,
Cursos, Grados y secciones), cada una con **Listar, Listar Estado, Crear, Listar ID, Editar, Eliminar Lógico y Restaurar
Lógico**. Ejecuta primero *Login (administrador)*, que guarda el token. Con el backend levantado:

```bash
postman collection run postman/Ugartino-Backend-CRUD.postman_collection.json   # 36 peticiones, 67 verificaciones
```

### IDs con saltos (1, 10, 10002…)

SQL Server guardaba en caché 1000 valores de IDENTITY y los perdía al reiniciarse. El esquema ya la desactiva
(`IDENTITY_CACHE = OFF`); para renumerar saltos que ya existan (con el backend detenido):

```bash
docker stop ugartino-backend
docker exec -i ugartino-sqlserver /opt/mssql-tools18/bin/sqlcmd -C -b -I -S localhost -U sa -P Admin12345 < database/mantenimiento/renumerar_ids.sql
docker start ugartino-backend
```

### Base creada con la versión anterior

Si tu `ugartino_db` (o `./datos-local`) se creó con el modelo anterior, las tablas antiguas siguen ahí junto a las nuevas.
Para quitarlas ejecuta a mano `database/migracion/eliminar_tablas_anteriores.sql` (borra sus datos), o con Docker
`docker compose down -v` para empezar con la base vacía. En el perfil `local`, borra la carpeta `./datos-local`.

---

## 2. Datos iniciales y de demostración

Por defecto la base arranca **vacía**: solo se crean las 42 aulas (Inicial, Primaria y Secundaria con secciones A, B y C)
y la cuenta del administrador `admin@ie20191.edu.pe` (contraseña **123456**); docentes, estudiantes, cursos y asistencia
se registran desde la API. Con `DATOS_DEMO=true` (y sin docentes registrados) se cargan además estos datos de ejemplo.
Contraseña: **123456**.

| Perfil | Ingreso |
|---|---|
| Administrador | `admin@ie20191.edu.pe` |
| Docente | `elena.salazar@`, `luis.mendoza@`, `carmen.rojas@`, `jorge.paredes@`, `rosa.huaman@`, `carlos.mendoza@` (+ `ie20191.edu.pe`) |
| Alumno (nombre + DNI) | **Juan Pérez Ugarte** · DNI **72819201** (1ro grado A de Primaria) |
| Alumno (correo) | `lucia.bustamante@ie20191.edu.pe` · `juan.alvarado@...` está **inactiva** (prueba de bloqueo) |

Datos cargados: 42 aulas (Inicial, Primaria y Secundaria con secciones A, B y C; la C en turno tarde), 9 docentes y
4 miembros del personal (directivo, administrativo, auxiliar y servicio), 505 estudiantes (12 por aula), 12 cursos
(1ro grado A de Primaria y 3ro año A de Secundaria), historial de asistencia diaria y de marcaciones del personal,
y una justificación pendiente.

---

## 3. Arquitectura

```
src/main/java/pe/edu/vallegrande/app
├─ config/        PropiedadesApp (incluye reglas de asistencia), CORS e interceptor, reloj del colegio, Swagger, DatosIniciales
├─ seguridad/     token firmado (HMAC-SHA256), contraseñas PBKDF2, @RequierePerfil, @Publico, interceptor
├─ model/         8 entidades JPA + enums (con el texto que usa el frontend: "presente", "Pendiente", "Mañana")
├─ repository/    Spring Data JPA
├─ service/       interfaces + impl/ con la lógica de negocio
├─ dto/           request (validaciones) y response
├─ rest/          controladores REST
├─ exception/     ApiException + GlobalExceptionHandler
└─ util/          Calendario (días hábiles), Textos
database/init/       00_base_datos.sql, 01_esquema.sql (fuente de verdad del esquema en SQL Server)
database/migracion/  eliminar_tablas_anteriores.sql (manual)
```

- **Esquema**: lo crea `01_esquema.sql`; la aplicación arranca con `ddl-auto=validate`, así que cualquier diferencia
  entre entidades y tablas impide el arranque. Las pruebas generan el esquema desde las entidades en H2.
- **Seguridad**: `POST /api/auth/login` devuelve un token; el resto de `/api/**` exige `Authorization: Bearer <token>`.
  Cada endpoint declara los perfiles permitidos; además el alumno solo accede a sus datos y el docente solo a sus cursos.
- **Errores**: siempre `{ status, mensaje, errores[], ruta, fecha }` con 400 (validación), 401 (sin sesión),
  403 (sin permiso), 404 (no existe), 409 (duplicado o en uso).

---

## 4. Modelo de datos

```
usuarios 1─0..1 docentes 1─* cursos *─1 grados_secciones 1─* estudiantes *─0..1 usuarios
                   │                                              │
                   └─* asistencia_personal          asistencia_estudiante *─0..1 cursos
                                                    justificaciones       *─0..1 cursos
```

| N.º | Tabla | Tipo | Contenido | Restricciones principales |
|---|---|---|---|---|
| 1 | `estudiantes` | Maestra | Datos del estudiante y su aula (`grado_seccion_id`) | DNI único de 8 dígitos; estado A/I (retiro lógico); cuenta vinculada única |
| 2 | `docentes` | Maestra | Docentes **y** demás personal (cargo Docente, Auxiliar, Administrativo, Directivo, Servicio) | DNI único; sexo M/F; celular de 9 dígitos; estado A/I (eliminación lógica) |
| 3 | `usuarios` | Maestra | Cuentas de acceso | Correo único; perfil y estado por CHECK; contraseña PBKDF2 |
| 4 | `grados_secciones` | Maestra | Nivel + grado + sección + turno | Combinación única; nivel Inicial/Primaria/Secundaria; turno Mañana/Tarde |
| 5 | `cursos` | Maestra | Curso dictado en un aula por un docente | Nombre único por aula; el docente debe tener cargo Docente |
| 6 | `asistencia_estudiante` | Transaccional | P/T/F/J; `curso_id` NULL = asistencia diaria del aula | Uno por estudiante, fecha y curso (índices filtrados); "pendiente" = sin fila |
| 7 | `asistencia_personal` | Transaccional | Entrada/salida del personal (tabla `docentes`) | Una por persona y fecha; estado puntual/tardanza; salida > entrada |
| 8 | `justificaciones` | Transaccional | Solicitudes y revisión (`curso_id` NULL = día completo) | Una pendiente por estudiante, fecha y curso (índice filtrado) |

---

## 5. Reglas de negocio

- **Asistencia de estudiantes**: no se registra en fechas futuras ni en fin de semana; solo estudiantes activos del aula.
  Un docente corrige hasta `app.asistencia.dias-edicion-docente` (5) días hábiles atrás; la de un curso solo la
  registra su docente (o el administrador).
- **Asistencia del personal**: después de `hora-entrada` (07:45) + `minutos-tolerancia` (10) la entrada es tardanza.
  Se configuran en `application.yml` o con las variables `HORA_ENTRADA`, `MINUTOS_TOLERANCIA`, `DIAS_EDICION_DOCENTE`.
- **Justificaciones**: se guarda el estado del día (o del curso) al solicitar. *Aprobar* lo marca `justificado` (salvo
  que estuviera presente). *Observar* devuelve el estado previo o, si no había, `falta`. Si tiene curso, la revisa su
  docente (o el administrador).
- **Bajas lógicas**: docentes/personal y estudiantes no se borran (estado I) y se pueden restaurar. Un docente con cursos
  no se elimina ni cambia de cargo; un aula con estudiantes o cursos no se elimina; un curso con asistencia no se elimina.
- **Usuarios**: correo único; no se puede desactivar/eliminar la propia cuenta ni al último administrador activo;
  una cuenta con registros se desactiva en lugar de eliminarse.

---

## 6. Endpoints

Base: `/api`. Perfiles: **A** administrador, **D** docente, **E** alumno.

| Recurso | Endpoints |
|---|---|
| Autenticación | `POST /auth/login` (público) · `GET /auth/sesion` |
| Usuarios (A) | `GET/POST /usuarios` · `GET/PUT/DELETE /usuarios/{id}` · `PATCH /usuarios/{id}/estado` · `PUT /usuarios/{id}/contrasena` (propia o A) |
| Grados y secciones | `GET /grados-secciones?nivel&grado` (A D) · `GET /grados-secciones/{id}` (A D) · `POST`, `PUT /{id}`, `DELETE /{id}` (A) |
| Docentes y personal | `GET /docentes?estado=A\|I\|T&cargo` (A) · `GET /docentes/{id}` (A, D propio) · `POST` · `PUT /{id}` · `PATCH /{id}/eliminar` · `PATCH /{id}/restaurar` (A) · `GET /docentes/{id}/cursos` (A, D propio) |
| Estudiantes | `GET /estudiantes?gradoSeccionId&estado&q` (A D) · `GET /estudiantes/{id}` (A D, E propio) · `POST` · `PUT /{id}` · `PATCH /{id}/eliminar` · `PATCH /{id}/restaurar` (A) · `PUT /{id}/contacto` (A, E propio) · `GET /{id}/asistencia?desde&hasta` (A D, E propio) |
| Cursos | `GET /cursos?gradoSeccionId&docenteId` (A D) · `GET /cursos/{id}` (A D) · `POST` · `PUT /{id}` · `DELETE /{id}` (A) |
| Asistencia de estudiantes (A D) | `GET/PUT /asistencia-estudiantes/grados-secciones/{id}?cursoId&fecha` · `GET /asistencia-estudiantes/grados-secciones/{id}/mensual?cursoId&mes=AAAA-MM` · `PUT /asistencia-estudiantes/estudiantes/{id}` |
| Asistencia del personal (A) | `GET /asistencia-personal?fecha` · `GET /asistencia-personal/reporte?mes` · `PUT /asistencia-personal/{docenteId}/entrada` · `PUT /asistencia-personal/{docenteId}/salida` · `DELETE /asistencia-personal/{docenteId}/{fecha}` |
| Justificaciones | `GET /justificaciones?estado&estudianteId&cursoId&docenteId&desde&hasta` · `POST` · `GET/PUT/DELETE /{id}` (A D E) · `PATCH /{id}/revision` (A D) |

Fechas `AAAA-MM-DD`, horas `HH:mm`, meses `AAAA-MM`.

> **Frontend:** esta versión reemplaza endpoints que usaba el frontend Angular (`/secciones`, `/matriculas`,
> `/asignaciones`, `/institucion`, `/comunicados`, `/alertas`, `/dashboard`, `/alumnos`, `/personal`, `/asistencia`).
> Las pantallas que dependan de ellos deben apuntar a los endpoints de esta tabla.
