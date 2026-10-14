export type NivelEducativo = 'Inicial' | 'Primaria' | 'Secundaria';

export type EstadoAsistencia = 'presente' | 'tardanza' | 'falta' | 'justificado' | 'pendiente';

export type EstadoJustificacion = 'Pendiente' | 'Aprobada' | 'Observada';

/** Estudiante matriculado (GET /api/matriculas). El id es el del backend, como texto. */
export interface Estudiante {
  id: string;
  nombre: string;
  dni: string;
  nivel: NivelEducativo;
  grado: string;
  seccion: string;
  matriculaId?: string;
  seccionId?: string;
  genero?: string;
  telefono?: string;
  correo?: string;
}

export interface Justificacion {
  id: number;
  estudianteId: string;
  estudianteNombre: string;
  fecha: string;
  cursoId?: string;
  cursoNombre?: string;
  motivo: string;
  detalle?: string;
  archivoNombre?: string;
  observacion?: string;
  estado: EstadoJustificacion;
  /** Estado de asistencia al momento de solicitarla; se restaura si la solicitud se observa. */
  estadoPrevio?: EstadoAsistencia;
  fechaSolicitud?: string;
}

export interface ResumenAlerta {
  estudiante: Estudiante;
  faltas: number;
  faltaHoy: boolean;
  nivel: 'critica' | 'preventiva' | 'hoy';
  ultimoAviso?: string;
}

export interface RegistroAsistencia extends Estudiante {
  fecha: string;
  estado: EstadoAsistencia;
}

/** Forma de EstudianteResponse del backend. */
export interface EstudianteApi {
  id: number;
  nombre: string;
  nombres: string;
  apellidos: string;
  dni: string;
  genero?: string;
  nivel: NivelEducativo;
  grado: string;
  gradoEtiqueta: string;
  seccion: string;
  seccionId: number;
  turno: 'Mañana' | 'Tarde';
  matriculaId: number;
  estadoMatricula: 'Activa' | 'Retirada';
  telefono?: string;
  correo?: string;
}

/** Forma de JustificacionResponse del backend. */
export interface JustificacionApi {
  id: number;
  estudianteId: number;
  estudianteNombre: string;
  fecha: string;
  cursoId?: number;
  cursoNombre?: string;
  motivo: string;
  detalle?: string;
  archivoNombre?: string;
  estado: EstadoJustificacion;
  estadoPrevio?: EstadoAsistencia;
  observacion?: string;
  fechaSolicitud: string;
}

export function aEstudiante(e: EstudianteApi): Estudiante {
  return {
    id: String(e.id), nombre: e.nombre, dni: e.dni, nivel: e.nivel, grado: e.grado, seccion: e.seccion,
    matriculaId: e.matriculaId ? String(e.matriculaId) : undefined, seccionId: e.seccionId ? String(e.seccionId) : undefined,
    genero: e.genero, telefono: e.telefono, correo: e.correo
  };
}

export function aJustificacion(j: JustificacionApi): Justificacion {
  return {
    id: j.id, estudianteId: String(j.estudianteId), estudianteNombre: j.estudianteNombre, fecha: j.fecha,
    cursoId: j.cursoId ? String(j.cursoId) : undefined, cursoNombre: j.cursoNombre, motivo: j.motivo, detalle: j.detalle,
    archivoNombre: j.archivoNombre, observacion: j.observacion, estado: j.estado, estadoPrevio: j.estadoPrevio,
    fechaSolicitud: j.fechaSolicitud
  };
}
