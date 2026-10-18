import { PerfilAcceso } from '../services/auth.service';
import { NombreIcono } from '../../shared/components/icono/icono.component';

export interface EnlaceNavegacion {
  ruta: string;
  etiqueta: string;
  icono: NombreIcono;
  perfil: PerfilAcceso;
  grupo: 'Principal' | 'Gestión' | 'Comunidad' | 'Sistema';
  alerta?: boolean;
  /** Se muestra al pie del menú lateral (configuración / perfil) en lugar de dentro de su grupo. */
  pie?: boolean;
}

/** Única fuente de verdad del menú lateral y de los títulos de página. */
export const NAVEGACION: EnlaceNavegacion[] = [
  // Administrador
  { perfil: 'administrador', grupo: 'Principal', ruta: '/administrador/dashboard', etiqueta: 'Dashboard principal', icono: 'panel' },
  { perfil: 'administrador', grupo: 'Principal', ruta: '/administrador/secciones', etiqueta: 'Selección de sección', icono: 'secciones' },
  { perfil: 'administrador', grupo: 'Principal', ruta: '/administrador/asistencia', etiqueta: 'Vista detallada y matriz', icono: 'matriz' },
  { perfil: 'administrador', grupo: 'Principal', ruta: '/administrador/alertas', etiqueta: 'Reportes de alertas', icono: 'reporte', alerta: true },
  { perfil: 'administrador', grupo: 'Principal', ruta: '/administrador/justificaciones', etiqueta: 'Justificaciones', icono: 'justificacion' },
  { perfil: 'administrador', grupo: 'Gestión', ruta: '/administrador/docentes', etiqueta: 'Docentes y personal', icono: 'perfil' },
  { perfil: 'administrador', grupo: 'Gestión', ruta: '/administrador/estudiantes', etiqueta: 'Estudiantes', icono: 'alumnos' },
  { perfil: 'administrador', grupo: 'Gestión', ruta: '/administrador/cursos', etiqueta: 'Cursos', icono: 'cursos' },
  { perfil: 'administrador', grupo: 'Gestión', ruta: '/administrador/grados-secciones', etiqueta: 'Grados y secciones', icono: 'estructura' },
  { perfil: 'administrador', grupo: 'Gestión', ruta: '/administrador/usuarios', etiqueta: 'Usuarios', icono: 'usuarios' },
  { perfil: 'administrador', grupo: 'Gestión', ruta: '/administrador/asistencia-personal', etiqueta: 'Asistencia del personal', icono: 'reloj' },
  { perfil: 'administrador', grupo: 'Sistema', ruta: '/administrador/fases', etiqueta: 'Fases del proyecto', icono: 'fases' },
  { perfil: 'administrador', grupo: 'Sistema', ruta: '/administrador/configuracion', etiqueta: 'Configuración y administración', icono: 'ajustes', pie: true },
  // Docente
  { perfil: 'docente', grupo: 'Principal', ruta: '/docente/dashboard', etiqueta: 'Dashboard principal', icono: 'panel' },
  { perfil: 'docente', grupo: 'Principal', ruta: '/docente/registrar-asistencia', etiqueta: 'Asistencia por curso', icono: 'asistencia' },
  { perfil: 'docente', grupo: 'Principal', ruta: '/docente/secciones', etiqueta: 'Selección de sección', icono: 'secciones' },
  { perfil: 'docente', grupo: 'Principal', ruta: '/docente/asistencia', etiqueta: 'Vista detallada y matriz', icono: 'matriz' },
  { perfil: 'docente', grupo: 'Principal', ruta: '/docente/alertas', etiqueta: 'Reportes de alertas', icono: 'reporte', alerta: true },
  { perfil: 'docente', grupo: 'Gestión', ruta: '/docente/historial', etiqueta: 'Historial', icono: 'reloj' },
  { perfil: 'docente', grupo: 'Gestión', ruta: '/docente/justificaciones', etiqueta: 'Justificaciones', icono: 'justificacion' },
  { perfil: 'docente', grupo: 'Gestión', ruta: '/docente/cursos', etiqueta: 'Mis cursos', icono: 'cursos' },
  { perfil: 'docente', grupo: 'Gestión', ruta: '/docente/estudiantes', etiqueta: 'Mis estudiantes', icono: 'alumnos' },
  { perfil: 'docente', grupo: 'Gestión', ruta: '/docente/reportes', etiqueta: 'Reportes', icono: 'tendencia' },
  { perfil: 'docente', grupo: 'Comunidad', ruta: '/docente/comunicados', etiqueta: 'Comunicados', icono: 'megafono' },
  { perfil: 'docente', grupo: 'Sistema', ruta: '/docente/perfil', etiqueta: 'Mi perfil', icono: 'perfil', pie: true },
  // Alumno
  { perfil: 'alumno', grupo: 'Principal', ruta: '/alumno/inicio', etiqueta: 'Inicio', icono: 'inicio' },
  { perfil: 'alumno', grupo: 'Principal', ruta: '/alumno/asistencia', etiqueta: 'Mi asistencia', icono: 'asistencia' },
  { perfil: 'alumno', grupo: 'Principal', ruta: '/alumno/justificaciones', etiqueta: 'Justificaciones', icono: 'justificacion' },
  { perfil: 'alumno', grupo: 'Gestión', ruta: '/alumno/cursos', etiqueta: 'Mis cursos', icono: 'cursos' },
  { perfil: 'alumno', grupo: 'Gestión', ruta: '/alumno/horario', etiqueta: 'Mi horario', icono: 'horario' },
  { perfil: 'alumno', grupo: 'Comunidad', ruta: '/alumno/alertas', etiqueta: 'Alertas', icono: 'alerta' },
  { perfil: 'alumno', grupo: 'Comunidad', ruta: '/alumno/comunicaciones', etiqueta: 'Comunicaciones', icono: 'mensaje' },
  { perfil: 'alumno', grupo: 'Sistema', ruta: '/alumno/perfil', etiqueta: 'Mi perfil', icono: 'perfil', pie: true }
];

/** Acción rápida del botón al pie del menú lateral, según el perfil. */
export const ACCION_RAPIDA: Partial<Record<PerfilAcceso, { ruta: string; etiqueta: string }>> = {
  docente: { ruta: '/docente/registrar-asistencia', etiqueta: 'Tomar asistencia' },
  alumno: { ruta: '/alumno/justificaciones', etiqueta: 'Nueva justificación' }
};

export const ETIQUETA_PERFIL: Record<PerfilAcceso, string> = {
  administrador: 'Administrador',
  docente: 'Docente',
  alumno: 'Alumno'
};
