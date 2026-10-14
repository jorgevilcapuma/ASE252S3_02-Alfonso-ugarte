import { Injectable, inject } from '@angular/core';
import { EstadoAsistencia, Estudiante, EstudianteApi, Justificacion, JustificacionApi, aEstudiante, aJustificacion } from '../../core/models/asistencia.model';
import { ApiService } from '../../core/services/api.service';
import { AsistenciaService } from '../../core/services/asistencia.service';
import { AuthService } from '../../core/services/auth.service';
import { NotificacionService } from '../../core/services/notificacion.service';
import { CursoAlumno } from './alumno.data';

export interface DiaAlumno {
  fecha: string;
  estado: EstadoAsistencia;
  cursoId?: string;
  cursoNombre?: string;
  docente?: string;
  hora?: string;
  observacion?: string;
}

export interface PerfilContactoAlumno {
  telefono: string;
  correo: string;
}

export interface AvisoAlumno {
  id: string;
  fecha: string;
  titulo: string;
  detalle: string;
  tipo: 'asistencia' | 'justificacion' | 'comunicado';
  leido: boolean;
}

interface CursoApi { id: number; nombre: string; codigo: string; docente: string; aula: string | null; gradoEtiqueta: string; seccion: string; nivel: string; horarios: { dia: string; inicio: string; fin: string }[]; }
interface DiaApi { fecha: string; estado: EstadoAsistencia; cursoId?: number; cursoNombre?: string; docente?: string; hora?: string; observacion?: string; }

type Opciones = { cursoId?: string; desde?: string; hasta?: string; limite?: number };

/** Portal del alumno: todo se obtiene del backend con el estudiante de la sesión. */
@Injectable({ providedIn: 'root' })
export class AlumnoService {
  private readonly auth = inject(AuthService);
  private readonly asistencia = inject(AsistenciaService);
  private readonly api = inject(ApiService);
  private readonly notificaciones = inject(NotificacionService);

  estudiante: Estudiante | undefined;
  cursos: CursoAlumno[] = [];
  justificaciones: Justificacion[] = [];
  alertas: AvisoAlumno[] = [];
  comunicaciones: AvisoAlumno[] = [];
  /** Registros por curso y días sin registro por curso, del año (más reciente primero). */
  private dias: DiaAlumno[] = [];
  /** Asistencia diaria del aula por fecha. */
  private diario: Record<string, EstadoAsistencia> = {};
  private carga: Promise<void> | null = null;

  constructor() {
    this.auth.alCerrarSesion(() => {
      this.estudiante = undefined;
      this.cursos = [];
      this.justificaciones = [];
      this.alertas = [];
      this.comunicaciones = [];
      this.dias = [];
      this.diario = {};
      this.carga = null;
    });
  }

  get hoy(): string { return this.asistencia.hoy; }
  get nombreInstitucion(): string { return this.asistencia.nombreInstitucion; }
  private get id(): string | undefined { return this.auth.sesion?.estudianteId; }
  private get inicioDelAnio(): string { return `${this.hoy.slice(0, 4)}-01-01`; }

  asegurar(): Promise<void> {
    this.carga ??= this.recargar().catch(error => {
      this.carga = null;
      this.notificaciones.error('No se pudieron cargar tus datos', ApiService.mensaje(error));
    });
    return this.carga;
  }

  async recargar(): Promise<void> {
    const id = this.id;
    if (!id) return;
    const [perfil, cursos] = await Promise.all([
      this.api.get<EstudianteApi>(`/alumnos/${id}/perfil`),
      this.api.get<CursoApi[]>(`/alumnos/${id}/cursos`),
      this.cargarAsistencia(),
      this.cargarJustificaciones(),
      this.cargarAvisos()
    ]);
    this.estudiante = aEstudiante(perfil);
    this.cursos = cursos.map(c => ({
      id: String(c.id), nombre: c.nombre, codigo: c.codigo, docente: c.docente, aula: c.aula ?? '',
      seccion: `${c.gradoEtiqueta} ${c.seccion}`, horario: c.horarios
    }));
  }

  private async cargarAsistencia(): Promise<void> {
    const [dias, diario] = await Promise.all([
      this.api.get<DiaApi[]>(`/alumnos/${this.id}/asistencia`, { desde: this.inicioDelAnio, hasta: this.hoy, limite: 100000 }),
      this.api.get<{ fecha: string; estado: EstadoAsistencia }[]>(`/estudiantes/${this.id}/asistencia`, { desde: this.inicioDelAnio, hasta: this.hoy })
    ]);
    this.dias = dias.map(d => ({ ...d, cursoId: d.cursoId ? String(d.cursoId) : undefined }));
    this.diario = Object.fromEntries(diario.map(d => [d.fecha, d.estado]));
  }

  private async cargarJustificaciones(): Promise<void> {
    this.justificaciones = (await this.api.get<JustificacionApi[]>('/justificaciones')).map(aJustificacion);
  }

  private async cargarAvisos(): Promise<void> {
    [this.alertas, this.comunicaciones] = await Promise.all([
      this.api.get<AvisoAlumno[]>(`/alumnos/${this.id}/alertas`),
      this.api.get<AvisoAlumno[]>(`/alumnos/${this.id}/comunicaciones`)
    ]);
  }

  get etiquetaSeccion(): string {
    const estudiante = this.estudiante;
    if (!estudiante) return 'Sección';
    const grado = estudiante.nivel === 'Inicial' ? estudiante.grado : `${estudiante.grado}°`;
    return `${grado} ${estudiante.nivel} ${estudiante.seccion}`;
  }

  get contacto(): PerfilContactoAlumno {
    return { telefono: this.estudiante?.telefono ?? '', correo: this.estudiante?.correo ?? this.auth.sesion?.correo ?? '' };
  }

  /** Historial por curso y día. Sin `desde`, devuelve los últimos 20 registros. */
  historial(opciones: Opciones = {}): DiaAlumno[] {
    const hasta = opciones.hasta ?? this.hoy;
    const limite = opciones.limite ?? (opciones.desde ? Number.MAX_SAFE_INTEGER : 20);
    return this.dias
      .filter(d => (!opciones.cursoId || d.cursoId === opciones.cursoId) && d.fecha <= hasta && (!opciones.desde || d.fecha >= opciones.desde))
      .slice(0, limite);
  }

  resumen(opciones: Opciones = {}) {
    const contados = this.historial(opciones).filter(item => item.estado !== 'pendiente');
    const presentes = contados.filter(item => item.estado === 'presente').length;
    const tardanzas = contados.filter(item => item.estado === 'tardanza').length;
    const faltas = contados.filter(item => item.estado === 'falta').length;
    const justificadas = contados.filter(item => item.estado === 'justificado').length;
    return {
      asistencias: presentes,
      tardanzas,
      faltas,
      justificadas,
      total: contados.length,
      porcentajeAsistencia: contados.length ? Math.round((presentes + tardanzas + justificadas) / contados.length * 100) : 0,
      porcentajeInasistencia: contados.length ? Math.round(faltas / contados.length * 100) : 0
    };
  }

  estadoHoy(cursoId?: string): EstadoAsistencia {
    return this.estadoEnFecha(this.hoy, cursoId);
  }

  /** Estado registrado por el docente del curso o, sin curso, el del registro diario del aula. */
  estadoEnFecha(fecha: string, cursoId?: string): EstadoAsistencia {
    if (cursoId) return this.dias.find(d => d.fecha === fecha && d.cursoId === cursoId)?.estado ?? 'pendiente';
    return this.diario[fecha] ?? 'pendiente';
  }

  /** Devuelve null si se envió o el mensaje de error (fecha futura, duplicada, etc.). */
  async solicitarJustificacion(fecha: string, motivo: string, archivoNombre?: string, cursoId?: string): Promise<string | null> {
    if (!this.id) return 'No hay un estudiante en la sesión.';
    const error = await this.asistencia.registrarJustificacion(this.id, fecha, motivo, archivoNombre, cursoId);
    if (!error) await Promise.all([this.cargarJustificaciones(), this.cargarAvisos()]);
    return error;
  }

  async guardarContacto(contacto: PerfilContactoAlumno): Promise<string | null> {
    if (!this.id) return 'No hay un estudiante en la sesión.';
    try {
      this.estudiante = aEstudiante(await this.api.put<EstudianteApi>(`/estudiantes/${this.id}/contacto`, contacto));
      return null;
    } catch (error) {
      return ApiService.mensaje(error);
    }
  }

  cursoPorId(id: string | null): CursoAlumno | undefined {
    return this.cursos.find(curso => curso.id === id);
  }

  etiquetaEstado(estado: EstadoAsistencia): string {
    return ({ presente: 'Asistió', tardanza: 'Tardanza', falta: 'Falta', justificado: 'Justificado', pendiente: 'Sin registrar' })[estado];
  }
}
