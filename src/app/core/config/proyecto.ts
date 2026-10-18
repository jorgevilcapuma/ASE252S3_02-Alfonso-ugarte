/** Fases del plan de trabajo de 8 semanas (no dependen del backend). */
export const FASES_PROYECTO: { semana: string; titulo: string; actividad: string; producto: string; estado: 'Completada' | 'En curso' | 'Pendiente' }[] = [
  { semana: '01', titulo: 'Exploración', actividad: 'Investigación de la comunicación dispersa y el control manual de asistencia.', producto: 'Ficha de diagnóstico institucional', estado: 'Completada' },
  { semana: '02', titulo: 'Planeación', actividad: 'Definición de objetivos, alcance, perfiles de usuario, herramientas y componentes del sistema.', producto: 'Plan del proyecto', estado: 'Completada' },
  { semana: '03', titulo: 'Requerimientos', actividad: 'Matriz transaccional, reglas de asistencia (tolerancia, plazos), justificaciones y alertas tempranas.', producto: 'Documento de requerimientos', estado: 'Completada' },
  { semana: '04', titulo: 'Diseño técnico', actividad: 'Wireframes, arquitectura Angular + Spring Boot y modelo relacional en SQL Server.', producto: 'Prototipo y modelo de base de datos', estado: 'Completada' },
  { semana: '05–06', titulo: 'Desarrollo', actividad: 'Portales de administrador, docente y alumno; API REST con permisos por perfil; asistencia diaria, por curso y del personal.', producto: 'Sistema web funcional', estado: 'Completada' },
  { semana: '07', titulo: 'Validación', actividad: 'Pruebas con docentes del colegio: tiempo de registro por aula, respuesta a justificaciones y detección temprana.', producto: 'Informe de validación con indicadores', estado: 'En curso' },
  { semana: '08', titulo: 'Presentación final', actividad: 'Corrección de observaciones, demostración del sistema y sustentación.', producto: 'Sistema final y presentación', estado: 'Pendiente' }
];
