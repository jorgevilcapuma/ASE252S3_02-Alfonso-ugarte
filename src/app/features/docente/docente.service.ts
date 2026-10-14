import { Injectable, inject, signal } from '@angular/core';
import { ApiService } from '../../core/services/api.service';
import { AuthService } from '../../core/services/auth.service';
import { NotificacionService } from '../../core/services/notificacion.service';

export interface PerfilDocente {
  id: string;
  nombres: string;
  apellidos: string;
  dni: string;
  email: string;
  telefono: string;
  especialidad: string;
  colegiatura?: string;
  codigoPlaza?: string;
  condicion?: string;
  nivelEducativo?: string;
  fechaIngreso?: string;
}

/** Resumen de una sesión de asistencia guardada por el docente. */
export interface SesionHistorial {
  id: string;
  fecha: string;
  cursoId: string;
  cursoNombre: string;
  gradoSeccion: string;
  asistieron: number;
  tardanzas: number;
  faltas: number;
  totalAlumnos: number;
}

export interface Curso {
  id: string;
  codigo: string;
  nombre: string;
  nivel: 'Inicial' | 'Primaria' | 'Secundaria';
  grado: string;
  seccion: string;
  horario: string;
  aula: string;
  totalAlumnos: number;
}

export interface Alumno {
  id: string;
  dni: string;
  nombres: string;
  apellidos: string;
  genero?: 'M' | 'F';
  nivel?: 'Inicial' | 'Primaria' | 'Secundaria';
  gradoSeccion?: string;
  curso?: string;
  cursoId: string;
  cursoNombre: string;
  porcentajeAsistencia: number;
  estadoGeneral: 'Normal' | 'En Riesgo' | 'Crítico';
}

export interface Justificacion {
  id: string;
  alumnoId: string;
  alumnoNombre: string;
  cursoId: string;
  cursoNombre: string;
  fechaClase: string;
  fechaSolicitud: string;
  motivo: string;
  observacionAlumno?: string;
  estado: 'Pendiente' | 'Aprobada' | 'Observada';
  respuestaDocente?: string;
}

export interface ComunicadoDocente {
  id: string;
  titulo: string;
  contenido: string;
  cursoId?: string;
  cursoNombre: string;
  prioridad: 'Alta' | 'Media' | 'Baja';
  fechaCreacion: string;
}

export interface RegistroAlumnoCurso {
  estado: 'asistio' | 'tardanza' | 'falta' | 'justificado';
  observacion: string;
}

/** Lista para tomar asistencia de un curso en una fecha (si no existe, todos aparecen como "asistio"). */
export interface SesionCurso {
  yaRegistrado: boolean;
  filas: { alumnoId: string; nombres: string; apellidos: string; dni: string; estado: RegistroAlumnoCurso['estado']; observacion: string }[];
}

export interface ResumenMensualAlumno { alumnoId: string; sesiones: number; asistencias: number; porcentaje: number; }

interface CursoApi { id: number; codigo: string; nombre: string; nivel: Curso['nivel']; grado: string; seccion: string; horario: string; aula: string | null; totalAlumnos: number; }
interface AlumnoApi { id: number; dni: string; nombres: string; apellidos: string; genero?: 'M' | 'F'; nivel: Curso['nivel']; gradoSeccion: string; cursoId: number; curso: string; cursoNombre: string; porcentajeAsistencia: number; estadoGeneral: Alumno['estadoGeneral']; }
interface JustificacionApi { id: number; estudianteId: number; estudianteNombre: string; cursoId?: number; cursoNombre?: string; fecha: string; fechaSolicitud: string; motivo: string; detalle?: string; estado: Justificacion['estado']; observacion?: string; }
interface ComunicadoApi { id: number; titulo: string; contenido: string; cursoId?: number; cursoNombre: string; prioridad: ComunicadoDocente['prioridad']; fechaCreacion: string; }

@Injectable({
  providedIn: 'root'
})
export class DocenteService {
  // Fecha local (toISOString() devolvía el día siguiente después de las 19:00 en Perú)
  readonly hoy: string = this.fechaLocal();
  private readonly auth = inject(AuthService);
  private readonly api = inject(ApiService);
  private readonly notificaciones = inject(NotificacionService);

  readonly perfil = signal<PerfilDocente | null>(null);
  readonly cursos = signal<Curso[]>([]);
  readonly alumnos = signal<Alumno[]>([]);
  readonly justificaciones = signal<Justificacion[]>([]);
  readonly comunicados = signal<ComunicadoDocente[]>([]);
  private readonly historial = signal<SesionHistorial[]>([]);
  private carga: Promise<void> | null = null;

  constructor() {
    this.auth.alCerrarSesion(() => {
      [this.cursos, this.alumnos, this.justificaciones, this.comunicados, this.historial].forEach(s => s.set([]));
      this.perfil.set(null);
      this.carga = null;
    });
  }

  private get docenteId(): string | undefined { return this.auth.sesion?.docenteId; }

  /** Carga el panel del docente con sesión iniciada (perfil, cursos, alumnos, historial, comunicados y justificaciones). */
  asegurar(): Promise<void> {
    this.carga ??= this.recargar().catch(error => {
      this.carga = null;
      this.notificaciones.error('No se pudieron cargar tus datos', ApiService.mensaje(error));
    });
    return this.carga;
  }

  async recargar(): Promise<void> {
    const id = this.docenteId;
    await Promise.all([this.cargarComunicados(), id ? this.cargarDocente(id) : Promise.resolve()]);
  }

  private async cargarDocente(id: string): Promise<void> {
    const [perfil, cursos] = await Promise.all([
      this.api.get<PerfilDocente & { id: number; fechaIngreso?: string }>(`/docentes/${id}`),
      this.api.get<CursoApi[]>(`/docentes/${id}/cursos`),
      this.cargarAlumnos(),
      this.cargarHistorial(),
      this.cargarJustificaciones()
    ]);
    this.perfil.set({ ...perfil, id: String(perfil.id), email: perfil.email ?? '', telefono: perfil.telefono ?? '', especialidad: perfil.especialidad ?? '' });
    this.cursos.set(cursos.map(c => ({
      id: String(c.id), codigo: c.codigo, nombre: c.nombre, nivel: c.nivel, grado: this.gradoCorto(c.nivel, c.grado),
      seccion: c.seccion, horario: c.horario, aula: c.aula ?? '', totalAlumnos: c.totalAlumnos
    })));
  }

  async cargarAlumnos(): Promise<void> {
    if (!this.docenteId) return;
    const lista = await this.api.get<AlumnoApi[]>(`/docentes/${this.docenteId}/estudiantes`);
    this.alumnos.set(lista.map(a => ({ ...a, id: String(a.id), cursoId: String(a.cursoId) })));
  }

  async cargarHistorial(): Promise<void> {
    if (!this.docenteId) return;
    const lista = await this.api.get<(Omit<SesionHistorial, 'id' | 'cursoId'> & { id: number; cursoId: number })[]>(`/docentes/${this.docenteId}/historial`);
    this.historial.set(lista.map(s => ({ ...s, id: String(s.id), cursoId: String(s.cursoId) })));
  }

  async cargarJustificaciones(): Promise<void> {
    if (!this.docenteId) return;
    const lista = await this.api.get<JustificacionApi[]>('/justificaciones', { docenteId: this.docenteId });
    this.justificaciones.set(lista.map(j => ({
      id: String(j.id), alumnoId: String(j.estudianteId), alumnoNombre: j.estudianteNombre, cursoId: j.cursoId ? String(j.cursoId) : '',
      cursoNombre: j.cursoNombre ?? 'Asistencia general', fechaClase: j.fecha, fechaSolicitud: j.fechaSolicitud.slice(0, 10),
      motivo: j.motivo, observacionAlumno: j.detalle, estado: j.estado, respuestaDocente: j.observacion
    })));
  }

  async cargarComunicados(): Promise<void> {
    const lista = await this.api.get<ComunicadoApi[]>('/comunicados');
    this.comunicados.set(lista.map(c => ({ ...c, id: String(c.id), cursoId: c.cursoId ? String(c.cursoId) : undefined })));
  }

  /** Perfil del docente con sesión iniciada; si la cuenta no tiene ficha, se arma con el nombre de la cuenta. */
  perfilDocente(): PerfilDocente {
    const perfil = this.perfil();
    if (perfil) return perfil;
    const partes = (this.auth.sesion?.nombre ?? 'Docente').split(' ').filter(Boolean);
    const corte = partes.length > 3 ? 2 : 1;
    return {
      id: this.docenteId ?? '', nombres: partes.slice(0, corte).join(' '), apellidos: partes.slice(corte).join(' '), dni: '',
      email: this.auth.sesion?.correo ?? '', telefono: '', especialidad: ''
    };
  }

  /** "Mi perfil": nombres, apellidos y teléfono. Devuelve null o el mensaje de error. */
  async guardarPerfil(perfil: PerfilDocente): Promise<string | null> {
    if (!this.docenteId) return 'Tu cuenta no está vinculada a una ficha docente. Comunícate con el administrador.';
    try {
      const guardado = await this.api.put<PerfilDocente & { id: number }>(`/docentes/${this.docenteId}/perfil`, {
        nombres: perfil.nombres, apellidos: perfil.apellidos, telefono: perfil.telefono
      });
      this.perfil.set({ ...guardado, id: String(guardado.id), email: guardado.email ?? '', telefono: guardado.telefono ?? '', especialidad: guardado.especialidad ?? '' });
      this.auth.actualizarNombre(`${guardado.nombres} ${guardado.apellidos}`);
      return null;
    } catch (error) {
      return ApiService.mensaje(error);
    }
  }

  // Métodos de Consulta y Filtrado
  alumnosPorCurso(cursoId: string): Alumno[] {
    if (!cursoId) return this.alumnos();
    return this.alumnos().filter(a => a.cursoId === cursoId);
  }

  cursosPorNivel(nivel: 'Inicial' | 'Primaria' | 'Secundaria'): Curso[] {
    return this.cursos().filter(c => c.nivel === nivel);
  }

  historialSesiones(): SesionHistorial[] {
    return this.historial();
  }

  // Acciones
  aprobarJustificacion(id: string, respuesta: string): Promise<string | null> {
    return this.revisar(id, 'Aprobada', respuesta);
  }

  rechazarJustificacion(id: string, respuesta: string): Promise<string | null> {
    return this.revisar(id, 'Observada', respuesta);
  }

  async agregarComunicado(nuevo: Omit<ComunicadoDocente, 'id' | 'fechaCreacion'>): Promise<string | null> {
    try {
      await this.api.post('/comunicados', {
        titulo: nuevo.titulo.trim(), contenido: nuevo.contenido.trim(), prioridad: nuevo.prioridad,
        cursoId: nuevo.cursoId ? Number(nuevo.cursoId) : undefined
      });
      await this.cargarComunicados();
      return null;
    } catch (error) {
      return ApiService.mensaje(error);
    }
  }

  async eliminarComunicado(id: string): Promise<string | null> {
    try {
      await this.api.delete(`/comunicados/${id}`);
      this.comunicados.update(lista => lista.filter(item => item.id !== id));
      return null;
    } catch (error) {
      return ApiService.mensaje(error);
    }
  }

  /** Asistencia de un curso en una fecha: si aún no se registró, cada alumno aparece como "asistio". */
  async obtenerSesion(cursoId: string, fecha: string): Promise<SesionCurso> {
    const r = await this.api.get<{ yaRegistrado: boolean; filas: (Omit<SesionCurso['filas'][number], 'alumnoId'> & { alumnoId: number })[] }>(
      `/asignaciones/${cursoId}/sesiones/${fecha}`);
    return { yaRegistrado: r.yaRegistrado, filas: r.filas.map(f => ({ ...f, alumnoId: String(f.alumnoId), observacion: f.observacion ?? '' })) };
  }

  /** Guarda la sesión del curso y actualiza historial y porcentajes. Devuelve null o el mensaje de error. */
  async guardarRegistroCurso(cursoId: string, fecha: string, registro: Record<string, RegistroAlumnoCurso>): Promise<string | null> {
    try {
      await this.api.put(`/asignaciones/${cursoId}/sesiones/${fecha}`, {
        registros: Object.entries(registro).map(([alumnoId, r]) => ({ estudianteId: Number(alumnoId), estado: r.estado, observacion: r.observacion }))
      });
      await Promise.all([this.cargarHistorial(), this.cargarAlumnos()]);
      return null;
    } catch (error) {
      return ApiService.mensaje(error);
    }
  }

  /** Reporte del mes (AAAA-MM) por alumno del curso. */
  async resumenMensual(cursoId: string, mes: string): Promise<ResumenMensualAlumno[]> {
    const lista = await this.api.get<{ alumnoId: number; sesiones: number; asistencias: number; porcentaje: number }[]>(
      `/asignaciones/${cursoId}/resumen-mensual`, { mes });
    return lista.map(r => ({ ...r, alumnoId: String(r.alumnoId) }));
  }

  private async revisar(id: string, estado: 'Aprobada' | 'Observada', observacion: string): Promise<string | null> {
    try {
      await this.api.patch(`/justificaciones/${id}/revision`, { estado, observacion });
      await this.cargarJustificaciones();
      return null;
    } catch (error) {
      return ApiService.mensaje(error);
    }
  }

  /** "3ro", "5to" o "5 años", como se mostraba en el panel docente. */
  private gradoCorto(nivel: Curso['nivel'], grado: string): string {
    if (nivel === 'Inicial') return grado;
    return ({ '1': '1ro', '2': '2do', '3': '3ro', '4': '4to', '5': '5to', '6': '6to' } as Record<string, string>)[grado] ?? `${grado}°`;
  }

  private fechaLocal(): string {
    const f = new Date();
    return `${f.getFullYear()}-${String(f.getMonth() + 1).padStart(2, '0')}-${String(f.getDate()).padStart(2, '0')}`;
  }
}
