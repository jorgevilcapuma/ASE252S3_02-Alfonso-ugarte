# Portal Ugartino Digital — Backend

API REST del sistema de asistencia escolar de la **I.E. 20191 Alfonso Ugarte**, construida a partir del frontend Angular
`ASE252S3_02-Alfonso-ugarte`, que ya está conectado a esta API: todas sus pantallas leen y guardan en la base de datos
SQL Server, con validaciones, permisos por perfil y reglas de negocio en el servidor.

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
| `DATOS_DEMO` / `DIAS_HISTORIAL` | `true` / `22` | Datos de demostración y días hábiles de historial |
| `ZONA_HORARIA` | `America/Lima` | Define el "hoy" de la asistencia |

### Pruebas

```bash
mvn test        # 81 pruebas: API completa sobre H2 (modo SQL Server) con reloj fijo
```

---

## 2. Cuentas de demostración

Son las mismas del frontend (`UsuariosService` y el login). Contraseña: **123456**.

| Perfil | Ingreso |
|---|---|
| Administrador | `admin@ie20191.edu.pe` |
| Docente | `elena.salazar@`, `luis.mendoza@`, `carmen.rojas@`, `jorge.paredes@`, `rosa.huaman@`, `carlos.mendoza@` (+ `ie20191.edu.pe`) |
| Alumno (nombre + DNI) | **Juan Pérez Ugarte** · DNI **72819201** (1° Primaria A) |
| Alumno (correo) | `lucia.bustamante@ie20191.edu.pe` · `juan.alvarado@...` está **inactiva** (prueba de bloqueo) |

Datos cargados: 3 niveles, 14 grados, 42 secciones (A, B, C; la C en turno tarde), 16 cursos, 9 docentes,
505 estudiantes (12 por aula, como `crearEstudiantes()` del frontend), 20 cursos por sección con horario, historial
de asistencia diaria y por curso, justificaciones pendientes, comunicados y las 7 fases del proyecto.

> **Ajuste respecto del frontend:** la lista de cursos de demostración del panel docente asignaba a un mismo profesor
> clases a la misma hora. El backend rechaza cruces de horario, así que esos cursos se repartieron entre docentes de
> la especialidad (el perfil "Carlos Antonio Mendoza Ríos" de *Mi perfil* tiene ahora sus cursos de Secundaria).

---

## 3. Arquitectura

```
src/main/java/pe/edu/vallegrande/app
├─ config/        PropiedadesApp, CORS e interceptor (WebConfig), reloj del colegio, Swagger, DatosIniciales
├─ seguridad/     token firmado (HMAC-SHA256), contraseñas PBKDF2, @RequierePerfil, @Publico, interceptor
├─ model/         entidades JPA + enums (con el mismo texto que usa el frontend: "presente", "Pendiente", "Mañana")
├─ repository/    Spring Data JPA (consultas con JOIN FETCH y agregados GROUP BY para alertas y dashboard)
├─ service/       interfaces + impl/ con la lógica de negocio
├─ dto/           request (validaciones) y response (forma de las interfaces TypeScript del frontend)
├─ rest/          controladores REST
├─ exception/     ApiException + GlobalExceptionHandler
└─ util/          Calendario (días hábiles), Textos
database/init/    00_base_datos.sql, 01_esquema.sql (fuente de verdad del esquema en SQL Server)
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
nivel_educativo 1─* grado 1─* seccion 1─* matricula *─1 estudiante *─0..1 usuario
                                  │                         │
curso 1─* asignacion *─1 seccion  ├─* asistencia_diaria     ├─* justificacion *─0..1 asignacion
          │  *─1 docente *─0..1 usuario                     ├─* aviso_familia
          ├─* horario                                       └─* asistencia_curso *─1 sesion_clase *─1 asignacion
          ├─* sesion_clase
          └─* comunicado *─0..1 usuario (autor)
institucion (1 fila)   fase_proyecto
```

| Tabla | Contenido | Restricciones principales |
|---|---|---|
| `institucion` | Nombre, código modular, dirección, colores, logo | Código de 7 dígitos, colores `#RRGGBB` |
| `nivel_educativo`, `grado`, `seccion` | Estructura del colegio | Nivel único; grado único por nivel; letra única por grado; turno Mañana/Tarde |
| `curso` | Catálogo de áreas | Nombre único |
| `asignacion`, `horario` | Curso dictado en una sección por un docente, con aula y bloques | Código único; un curso por sección; `hora_fin > hora_inicio`; sin cruces (servicio) |
| `usuario` | Cuentas | Correo único; perfil y estado por CHECK; contraseña PBKDF2 |
| `docente`, `estudiante` | Fichas personales (el estudiante incluye género M/F) | DNI único de 8 dígitos; cuenta vinculada única (índice filtrado) |
| `matricula` | Aula del estudiante por año | Una por estudiante y año; estado Activa/Retirada |
| `asistencia_diaria` | Matriz P/T/F/J | Una por estudiante y fecha ("pendiente" = sin fila) |
| `sesion_clase`, `asistencia_curso` | Asistencia por curso | Una sesión por curso y fecha; un registro por alumno y sesión |
| `justificacion` | Solicitudes y revisión | Una pendiente por estudiante, fecha y curso (índice filtrado) |
| `aviso_familia` | Historial de avisos de alertas | — |
| `comunicado` | Avisos de docentes | Prioridad Alta/Media/Baja; curso opcional (sin curso = todos) |
| `fase_proyecto` | Fases del proyecto | Orden único |
| `personal` | Personal del colegio (docente, auxiliar, administrativo, directivo, servicio) | DNI único; sexo M/F; celular de 9 dígitos; estado A/I con fechas de auditoría |

---

## 5. Reglas de negocio (tomadas del frontend)

- **Asistencia**: no se registra en fechas futuras ni en fin de semana; solo alumnos matriculados en el aula/curso.
- **Justificaciones**: se guarda el estado del día al solicitar. *Aprobar* marca el día como `justificado` (salvo que
  estuviera presente) y la falta/tardanza del curso como justificada. *Observar* devuelve el estado previo o, si no
  había, `falta`. Solo se revisan las pendientes; si tiene curso, la revisa su docente (o el administrador).
- **Alertas tempranas**: faltas en los últimos **20 días hábiles**: 3+ crítica, 2 preventiva, o falta hoy.
- **Mis estudiantes / alertas por curso**: porcentaje = sesiones sin falta / sesiones (Normal ≥ 80 %, En Riesgo ≥ 68 %).
- **Matrícula**: DNI de 8 dígitos y único; grado válido para el nivel; el traslado conserva el historial; anular elimina
  solo si no hay historial (si lo hay, retira la matrícula).
- **Usuarios**: correo único; no se puede desactivar/eliminar la propia cuenta ni al último administrador activo;
  una cuenta con registros se desactiva en lugar de eliminarse.

---

## 6. Endpoints por pantalla del frontend

Base: `/api`. Perfiles: **A** administrador, **D** docente, **E** alumno.

| Pantalla / servicio del frontend | Endpoints |
|---|---|
| Login (`AuthService`) | `POST /auth/login` (público) · `GET /auth/sesion` |
| Configuración (`TemaService`, datos del colegio) | `GET /institucion` (público) · `PUT /institucion` A · `POST /institucion/restablecer-identidad` A |
| Estructura / Selección de sección | `GET /estructura` · CRUD `/niveles`, `/grados`, `/secciones` (A) · `GET /secciones/{id}/estudiantes` A D |
| Usuarios (`UsuariosService`) | `GET/POST /usuarios` · `GET/PUT/DELETE /usuarios/{id}` · `PATCH /usuarios/{id}/estado` (A) · `PUT /usuarios/{id}/contrasena` (propia o A) |
| Matrículas | `GET /matriculas?nivel&grado&seccion&q&estado&pagina&tamanio` · `GET /matriculas/resumen` · `POST /matriculas` · `PUT /matriculas/{id}/traslado` · `PATCH /matriculas/{id}/retiro` · `PATCH /matriculas/{id}/reincorporacion` · `DELETE /matriculas/{id}` |
| Estudiantes | `GET/PUT/DELETE /estudiantes/{id}` · `PUT /estudiantes/{id}/contacto` (E propio) · `GET /estudiantes/{id}/asistencia` · `GET /estudiantes/{id}/avisos` |
| Matriz de asistencia (diaria y mensual) | `GET /asistencia?desde&hasta&seccionId` (rango, máx. 93 días) · `GET/PUT /asistencia/secciones/{seccionId}?fecha` · `GET /asistencia/secciones/{seccionId}/mensual?mes=AAAA-MM` · `PUT /asistencia/estudiantes/{id}/{fecha}` (A D) |
| Dashboard | `GET /dashboard?fecha` (A D) |
| Alertas | `GET /alertas` · `GET /alertas/tendencia?dias=7` · `POST /alertas/avisos` (A D) |
| Justificaciones | `GET /justificaciones?estado&estudianteId&cursoId&docenteId&desde&hasta` · `POST` · `GET/PUT/DELETE /{id}` · `PATCH /{id}/revision` (A D) |
| Cursos (catálogo) | CRUD `/cursos` (A; lectura D) |
| Mis cursos / horario (docente) | CRUD `/asignaciones` (A; lectura D: solo los suyos) · `GET /docentes/{id}/cursos` |
| Asistencia por curso | `GET/PUT/DELETE /asignaciones/{id}/sesiones/{fecha}` · `GET /asignaciones/{id}/sesiones` |
| Historial / Reportes (docente) | `GET /docentes/{id}/historial` · `GET /asignaciones/{id}/resumen-mensual?mes` |
| Mis estudiantes / alertas por curso | `GET /docentes/{id}/estudiantes?cursoId` · `GET /docentes/{id}/alertas` |
| Mi perfil (docente) | `GET /docentes/{id}` · `PUT /docentes/{id}/perfil` · CRUD `/docentes` (A) |
| Comunicados | `GET /comunicados?cursoId` (A D E) · `POST` · `GET/PUT/DELETE /{id}` (A D, autor) |
| Portal del alumno | `GET /alumnos/{id}/perfil` · `/inicio` · `/cursos` · `/asistencia?cursoId&desde&hasta&limite` · `/asistencia/resumen` · `/alertas` · `/comunicaciones` |
| Fases del proyecto | `GET /fases` · CRUD (A) |
| Personal (tabla maestra, A) | `GET /personal` · `GET /personal/estado/{A\|I\|T}` · `GET /personal/{id}` · `POST /personal` · `PUT /personal/{id}` · `PATCH /personal/eliminar/{id}` · `PATCH /personal/restaurar/{id}` |

La colección `postman/Personal.postman_collection.json` prueba esos 7 endpoints (más el login que guarda el token):
`postman collection run postman/Personal.postman_collection.json`. El personal no se borra: eliminar lo pasa a
estado `I` (`deletedAt`) y restaurar a `A` (`restoredAt`); no se puede editar mientras está eliminado.

### Integración con el frontend

El frontend (`ASE252S3_02-Alfonso-ugarte`) ya consume esta API:

- `core/config/api.ts` define la URL (`http://localhost:8080/api`); `core/interceptors/auth.interceptor.ts` envía el token
  y vuelve al login si la sesión expira (401).
- `AsistenciaService`, `UsuariosService`, `TemaService`, `DocenteService` y `AlumnoService` cargan los datos desde la API,
  los mantienen en memoria para las pantallas y guardan cada cambio en el backend (si el servidor lo rechaza, se revierte
  y se muestra su mensaje).
- Los ids del backend se manejan como texto en el frontend; los valores de catálogo viajan igual en ambos
  (`presente`, `asistio`, `Pendiente`, `Alta`, `Mañana`, `docente`). Fechas `AAAA-MM-DD`, horas `HH:mm`, meses `AAAA-MM`.
- Si el frontend se sirve en otro puerto, agrégalo a `CORS_ORIGINS` (por ejemplo `http://localhost:4200,http://localhost:4201`).
