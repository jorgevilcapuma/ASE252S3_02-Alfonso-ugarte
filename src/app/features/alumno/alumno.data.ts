/** Curso de la sección del alumno con su docente y horario (GET /api/alumnos/{id}/cursos). */
export interface CursoAlumno {
  id: string;
  nombre: string;
  codigo: string;
  docente: string;
  seccion: string;
  aula: string;
  horario: { dia: string; inicio: string; fin: string }[];
}

export const DIAS_CLASE = ['Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes'];
