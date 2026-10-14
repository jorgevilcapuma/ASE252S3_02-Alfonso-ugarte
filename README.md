# Portal Ugartino Digital — I.E. 20191 Alfonso Ugarte

Sistema de asistencia escolar (Angular 19): registro por aula, justificaciones, alertas tempranas y portales por perfil (administrador, docente, alumno).

## Ejecutar

```bash
npm install
npm start          # http://localhost:4200
npm run build      # build de producción
```

El portal funciona de **dos maneras** (se elige en la pantalla de ingreso, "Origen de los datos"):

| Modo | Qué necesita | Dónde quedan los datos |
|---|---|---|
| **Servidor (API)** | El backend **ASE252S3_BE** en http://localhost:8080 | Base de datos del backend (SQL Server, o H2 con el perfil `local`) |
| **Local (sin servidor)** | Nada más que `npm start` | En este navegador (`localStorage`) |

- Al abrir el portal sin un modo elegido, se usa el servidor si responde y, si no, el **modo local** automáticamente.
- El modo local atiende las mismas rutas que la API (`src/app/core/local/api-local.ts`) con los mismos datos de
  demostración y las mismas reglas (DNI y correos únicos, fechas válidas, justificaciones, alertas, permisos por perfil).
  "Reiniciar datos de demostración" en el login vuelve al estado inicial.
- Para levantar el backend: `docker compose up -d --build` (SQL Server) o `mvn spring-boot:run "-Dspring-boot.run.profiles=local"`
  (sin Docker) en `../ASE252S3_BE`.
- La dirección de la API y el modo por defecto (`auto`, `servidor` o `local`) están en `src/app/core/config/api.ts`.

Cuentas de demostración (contraseña **123456**), que se completan al elegir el perfil en el login:
`admin@ie20191.edu.pe`, `elena.salazar@ie20191.edu.pe` (docente) y el alumno **Juan Pérez Ugarte** · DNI **72819201**.
Más cuentas en el README del backend. En ambos modos los datos iniciales traen ~22 días lectivos de historial.

```bash
npm test -- --watch=false --browsers=ChromeHeadless   # pruebas unitarias
```

## Estructura

```
src/
├─ styles.css                  # punto de entrada (fuentes locales + imports)
├─ styles/
│  ├─ tokens.css               # colores, tipografía, radios, sombras (identidad del colegio)
│  ├─ base.css                 # reset y estilos base
│  ├─ components.css           # botones, tablas, KPIs, badges, modal, formularios
│  ├─ animations.css           # animaciones y microinteracciones
│  └─ legacy.css               # estilos heredados del portal alumno (migrar a CSS por componente)
└─ app/
   ├─ core/
   │  ├─ config/navegacion.ts  # ÚNICA fuente del menú lateral: agregar un módulo = 1 línea
   │  ├─ guards/perfil.guard.ts
   │  ├─ models/               # tipos compartidos
   │  ├─ interceptors/         # envía el token de la sesión a la API
   │  ├─ local/                # API simulada del modo local (sin servidor)
   │  └─ services/             # api (HTTP), auth, asistencia, usuarios, tema (colores/logo), notificaciones
   ├─ layout/shell/            # sidebar + topbar + footer
   ├─ shared/components/       # icono (SVG), pantalla-carga (splash / autenticando), toast-host, modulo-en-desarrollo
   └─ features/
      ├─ auth/login
      ├─ dashboard             # resumen institucional (administrador)
      ├─ asistencia/seleccion-seccion   # elegir nivel, grado y sección
      ├─ asistencia/matriz-asistencia   # registro diario P/T/F/J y vista mensual
      ├─ justificaciones
      ├─ alertas                # tendencia 7 días, alertas críticas/preventivas, avisos a familias
      ├─ configuracion          # datos del colegio, usuarios, estructura, identidad visual
      ├─ proyecto/fases
      ├─ administrador/        # rutas del perfil (lazy)
      ├─ docente/              # rutas + pantallas del perfil docente
      └─ alumno/               # rutas + pantallas del perfil alumno (incl. mi-asistencia)
```

## Cómo agregar un módulo nuevo

1. Crear `features/<modulo>/<modulo>.component.{ts,html,css}` (standalone, con `styleUrl`).
2. Registrar la ruta en `features/<perfil>/<perfil>.routes.ts`.
3. Añadir una entrada en `core/config/navegacion.ts` (etiqueta, ícono, grupo).

Mientras el módulo no esté listo se puede usar `ModuloEnDesarrolloComponent` (ver `administrador.routes.ts`).

## Diseño

Granate institucional (`--color-primary`) + dorado del escudo (`--color-gold`) sobre lienzo claro. Todos los valores salen de `styles/tokens.css`; los tonos suaves/oscuros se derivan con `color-mix`, así que cambiar los dos colores en **Configuración → Identidad visual** actualiza todo el portal. No usar colores sueltos en los componentes.
