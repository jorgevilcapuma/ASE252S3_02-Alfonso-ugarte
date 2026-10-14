import { Injectable, inject, signal } from '@angular/core';
import {
  EstadoAsistencia, EstadoJustificacion, Estudiante, EstudianteApi, Justificacion, JustificacionApi, NivelEducativo, ResumenAlerta,
  aEstudiante, aJustificacion
} from '../models/asistencia.model';
import { ApiService } from './api.service';
import { AuthService } from './auth.service';
import { NotificacionService } from './notificacion.service';
import { TemaService } from './tema.service';

/** Valores por defecto mientras llega la configuración del backend (los mismos que siembra la base de datos). */
export const VENTANA_ALERTAS = 20;
export const FALTAS_CRITICAS = 3;
export const FALTAS_PREVENTIVAS = 2;

export interface FaseProyecto { semana: string; titulo: string; actividad: string; producto: string; estado: string; }

interface SeccionInfo { id: string; nivel: NivelEducativo; grado: string; letra: string; turno: 'Mañana' | 'Tarde'; total: number; }

interface EstructuraApi {
  nombre: NivelEducativo;
  grados: { codigo: string; etiqueta: string; secciones: { id: number; letra: string; turno: 'Mañana' | 'Tarde'; totalEstudiantes: number }[] }[];
}
interface InstitucionApi { nombre: string; codigoModular: string; direccion: string; colorPrincipal: string; colorSecundario: string; logo: string; }
interface RegistroDiaApi { estudianteId: number; fecha: string; estado: EstadoAsistencia; }
interface AlertasApi {
  parametros: { ventanaDias: number; faltasCriticas: number; faltasPreventivas: number };
  alertas: { estudiante: EstudianteApi; faltas: number; faltaHoy: boolean; nivel: ResumenAlerta['nivel']; ultimoAviso?: string }[];
}

/**
 * Datos del módulo de asistencia, obtenidos del backend y guardados en memoria para que las pantallas
 * los consulten de forma inmediata. Cada cambio se envía a la API y, si falla, se revierte.
 */
@Injectable({ providedIn: 'root' })
export class AsistenciaService {
  private readonly api = inject(ApiService);
  private readonly auth = inject(AuthService);
  private readonly tema = inject(TemaService);
  private readonly notificaciones = inject(NotificacionService);

  /** Cambia con cada carga o modificación (para los `computed` de las pantallas). */
  readonly version = signal(0);
  readonly hoy = this.fechaLocal();

  niveles: NivelEducativo[] = ['Inicial', 'Primaria', 'Secundaria'];
  gradosPorNivel: Record<NivelEducativo, string[]> = {
    Inicial: ['3 años', '4 años', '5 años'],
    Primaria: ['1', '2', '3', '4', '5', '6'],
    Secundaria: ['1', '2', '3', '4', '5']
  };
  secciones = ['A', 'B', 'C'];
  fases: FaseProyecto[] = [];

  estudiantes: Estudiante[] = [];
  asistencia: Record<string, EstadoAsistencia> = {};
  justificaciones: Justificacion[] = [];
  alertas: ResumenAlerta[] = [];
  ventanaAlertas = VENTANA_ALERTAS;
  faltasCriticas = FALTAS_CRITICAS;
  faltasPreventivas = FALTAS_PREVENTIVAS;

  nombreInstitucion = 'I.E. 20191 Alfonso Ugarte';
  direccionInstitucion = 'San Vicente de Cañete, Lima, Perú';
  codigoModular = '0285940';

  private seccionesInfo: SeccionInfo[] = [];
  private etiquetas: Record<string, string> = {};
  private cargaBase: Promise<void> | null = null;
  private cargaGestion: Promise<void> | null = null;
  private recargaAlertas: ReturnType<typeof setTimeout> | null = null;

  constructor() {
    this.auth.alCerrarSesion(() => {
      this.estudiantes = [];
      this.asistencia = {};
      this.justificaciones = [];
      this.alertas = [];
      this.cargaBase = null;
      this.cargaGestion = null;
      this.cambio();
    });
  }

  // =====================================================================
  // Carga desde el backend
  // =====================================================================

  /** Datos públicos del colegio (nombre e identidad visual): se usan incluso en el login. */
  async cargarInstitucion(): Promise<void> {
    try {
      const i = await this.api.get<InstitucionApi>('/institucion');
      this.nombreInstitucion = i.nombre;
      this.direccionInstitucion = i.direccion;
      this.codigoModular = i.codigoModular;
      this.tema.establecer({ colorPrincipal: i.colorPrincipal, colorSecundario: i.colorSecundario, logo: i.logo ?? '' });
      this.cambio();
    } catch { /* sin conexión: se mantienen los valores conocidos */ }
  }

  /** Estructura académica y fases: para cualquier perfil con sesión. */
  asegurarBase(): Promise<void> {
    this.cargaBase ??= Promise.all([this.cargarEstructura(), this.cargarFases()]).then(() => undefined).catch(error => {
      this.cargaBase = null;
      throw error;
    });
    return this.cargaBase;
  }

  /** Padrón, asistencia de hoy, justificaciones y alertas: administrador y docente. */
  asegurarGestion(): Promise<void> {
    this.cargaGestion ??= Promise.all([
      this.asegurarBase(), this.cargarEstudiantes(), this.cargarAsistencia(this.hoy, this.hoy), this.cargarJustificaciones(), this.cargarAlertas()
    ]).then(() => undefined).catch(error => {
      this.cargaGestion = null;
      this.notificaciones.error('No se pudieron cargar los datos', ApiService.mensaje(error));
    });
    return this.cargaGestion;
  }

  async cargarEstructura(): Promise<void> {
    const niveles = await this.api.get<EstructuraApi[]>('/estructura');
    this.niveles = niveles.map(n => n.nombre);
    this.gradosPorNivel = Object.fromEntries(niveles.map(n => [n.nombre, n.grados.map(g => g.codigo)])) as Record<NivelEducativo, string[]>;
    this.etiquetas = {};
    this.seccionesInfo = [];
    for (const n of niveles) {
      for (const g of n.grados) {
        this.etiquetas[`${n.nombre}|${g.codigo}`] = g.etiqueta;
        g.secciones.forEach(s => this.seccionesInfo.push({ id: String(s.id), nivel: n.nombre, grado: g.codigo, letra: s.letra, turno: s.turno, total: s.totalEstudiantes }));
      }
    }
    this.secciones = [...new Set(this.seccionesInfo.map(s => s.letra))].sort();
    this.cambio();
  }

  async cargarFases(): Promise<void> {
    this.fases = await this.api.get<FaseProyecto[]>('/fases');
    this.cambio();
  }

  async cargarEstudiantes(): Promise<void> {
    const todos: Estudiante[] = [];
    for (let pagina = 1, total = 1; pagina <= total; pagina++) {
      const r = await this.api.get<{ contenido: EstudianteApi[]; totalPaginas: number }>('/matriculas', { pagina, tamanio: 1000 });
      todos.push(...r.contenido.map(aEstudiante));
      total = r.totalPaginas;
    }
    this.estudiantes = todos;
    this.cambio();
  }

  /** Trae la asistencia diaria del rango (todas las aulas o una sección) y reemplaza esa parte de la memoria. */
  async cargarAsistencia(desde: string, hasta: string, seccionId?: string): Promise<void> {
    const registros = await this.api.get<RegistroDiaApi[]>('/asistencia', { desde, hasta, seccionId });
    const deLaSeccion = seccionId ? new Set(this.estudiantes.filter(e => e.seccionId === seccionId).map(e => e.id)) : null;
    for (const clave of Object.keys(this.asistencia)) {
      const [fecha, id] = clave.split('|');
      if (fecha >= desde && fecha <= hasta && (!deLaSeccion || deLaSeccion.has(id))) delete this.asistencia[clave];
    }
    registros.forEach(r => this.asistencia[this.clave(String(r.estudianteId), r.fecha)] = r.estado);
    this.cambio();
  }

  async cargarJustificaciones(): Promise<void> {
    this.justificaciones = (await this.api.get<JustificacionApi[]>('/justificaciones')).map(aJustificacion);
    this.cambio();
  }

  async cargarAlertas(): Promise<void> {
    const r = await this.api.get<AlertasApi>('/alertas');
    this.ventanaAlertas = r.parametros.ventanaDias;
    this.faltasCriticas = r.parametros.faltasCriticas;
    this.faltasPreventivas = r.parametros.faltasPreventivas;
    this.alertas = r.alertas.map(a => ({ estudiante: aEstudiante(a.estudiante), faltas: a.faltas, faltaHoy: a.faltaHoy, nivel: a.nivel, ultimoAviso: a.ultimoAviso }));
    this.cambio();
  }

  // =====================================================================
  // Consultas sobre los datos cargados
  // =====================================================================

  get cantidadAlertas(): number {
    return this.estudiantes.filter(estudiante => this.obtenerEstado(estudiante.id, this.hoy) === 'falta').length;
  }

  get porcentajeRegistro(): number {
    if (this.estudiantes.length === 0) return 0;
    const registrados = this.estudiantes.filter(estudiante => this.obtenerEstado(estudiante.id, this.hoy) !== 'pendiente').length;
    return Math.round(registrados / this.estudiantes.length * 100);
  }

  obtenerEstado(estudianteId: string, fecha: string): EstadoAsistencia {
    return this.asistencia[this.clave(estudianteId, fecha)] ?? 'pendiente';
  }

  /** Estado guardado o undefined si ese día nunca se registró. */
  estadoRegistrado(estudianteId: string, fecha: string): EstadoAsistencia | undefined {
    return this.asistencia[this.clave(estudianteId, fecha)];
  }

  estudiantesDeAula(nivel: NivelEducativo, grado: string, seccion: string): Estudiante[] {
    return this.estudiantes.filter(item => item.nivel === nivel && item.grado === grado && item.seccion === seccion);
  }

  seccionId(nivel: NivelEducativo, grado: string, letra: string): string | undefined {
    return this.seccionesInfo.find(s => s.nivel === nivel && s.grado === grado && s.letra === letra)?.id;
  }

  /** Turno de la sección según la estructura del backend (por regla, la C funciona en la tarde). */
  turnoSeccion(seccion: string): 'Mañana' | 'Tarde' {
    return this.seccionesInfo.find(s => s.letra === seccion)?.turno ?? (seccion === 'C' ? 'Tarde' : 'Mañana');
  }

  etiquetaGrado(nivel: NivelEducativo, grado: string): string {
    const etiqueta = this.etiquetas[`${nivel}|${grado}`];
    if (etiqueta) return etiqueta;
    if (nivel === 'Inicial') return grado;
    const ordinal = ({ '1': '1ro', '2': '2do', '3': '3ro', '4': '4to', '5': '5to', '6': '6to' } as Record<string, string>)[grado] ?? `${grado}°`;
    return `${ordinal} ${nivel === 'Secundaria' ? 'año' : 'grado'}`;
  }

  /** Días lectivos (lunes a viernes) hasta `hasta`, del más antiguo al más reciente. */
  diasHabiles(cantidad: number, hasta = this.hoy): string[] {
    const dias: string[] = [];
    const cursor = new Date(`${hasta}T12:00:00`);
    while (dias.length < cantidad) {
      const dia = cursor.getDay();
      if (dia !== 0 && dia !== 6) dias.unshift(this.fechaIso(cursor));
      cursor.setDate(cursor.getDate() - 1);
    }
    return dias;
  }

  /** Todas las fechas de un mes (formato AAAA-MM). */
  diasDelMes(mes: string): string[] {
    const [anio, numero] = mes.split('-').map(Number);
    const total = new Date(anio, numero, 0).getDate();
    return Array.from({ length: total }, (_, indice) => `${mes}-${String(indice + 1).padStart(2, '0')}`);
  }

  esFinDeSemana(fecha: string): boolean {
    const dia = new Date(`${fecha}T12:00:00`).getDay();
    return dia === 0 || dia === 6;
  }

  contarEstado(estudianteId: string, fechas: string[], estado: EstadoAsistencia): number {
    return fechas.reduce((total, fecha) => total + (this.obtenerEstado(estudianteId, fecha) === estado ? 1 : 0), 0);
  }

  /** Faltas sin justificar por día, para las fechas dadas (deben estar cargadas). */
  faltasPorDia(fechas: string[]): { fecha: string; faltas: number }[] {
    return fechas.map(fecha => ({
      fecha,
      faltas: this.estudiantes.reduce((total, estudiante) => total + (this.obtenerEstado(estudiante.id, fecha) === 'falta' ? 1 : 0), 0)
    }));
  }

  /** Alertas calculadas por el backend (ventana de días hábiles, faltas críticas y preventivas). */
  resumenAlertas(): ResumenAlerta[] {
    return this.alertas;
  }

  estudiantesPorNivel(nivel: NivelEducativo): Estudiante[] {
    return this.estudiantes.filter(estudiante => estudiante.nivel === nivel);
  }

  porcentajeRegistroNivel(nivel: NivelEducativo): number {
    const estudiantes = this.estudiantesPorNivel(nivel);
    const registrados = estudiantes.filter(estudiante => this.obtenerEstado(estudiante.id, this.hoy) !== 'pendiente').length;
    return estudiantes.length === 0 ? 0 : Math.round(registrados / estudiantes.length * 100);
  }

  /** En el backend toda matrícula se puede anular: si tiene historial, se retira en lugar de borrarse. */
  esMatriculaManual(_estudianteId: string): boolean {
    return true;
  }

  // =====================================================================
  // Cambios (se guardan en el backend)
  // =====================================================================

  /** Guarda el estado de un día; "pendiente" borra el registro. Se ve al instante y se revierte si el servidor lo rechaza. */
  async guardarEstado(estudianteId: string, fecha: string, estado: EstadoAsistencia): Promise<boolean> {
    const clave = this.clave(estudianteId, fecha);
    const anterior = this.asistencia[clave];
    this.fijar(clave, estado);
    try {
      await this.api.put(`/asistencia/estudiantes/${estudianteId}/${fecha}`, { estado });
      this.programarAlertas();
      return true;
    } catch (error) {
      this.fijar(clave, anterior ?? 'pendiente');
      this.notificaciones.error('No se guardó la asistencia', ApiService.mensaje(error));
      return false;
    }
  }

  /** Guarda varios estudiantes de una sección en una sola petición. */
  async guardarEstados(seccionId: string, fecha: string, registros: { estudianteId: string; estado: EstadoAsistencia }[]): Promise<string | null> {
    if (registros.length === 0) return null;
    try {
      await this.api.put(`/asistencia/secciones/${seccionId}`, {
        fecha, registros: registros.map(r => ({ estudianteId: Number(r.estudianteId), estado: r.estado }))
      });
      registros.forEach(r => this.fijar(this.clave(r.estudianteId, fecha), r.estado));
      this.programarAlertas();
      return null;
    } catch (error) {
      return ApiService.mensaje(error);
    }
  }

  async registrarAvisoFamilia(estudianteIds: string[]): Promise<string | null> {
    try {
      await this.api.post('/alertas/avisos', { estudianteIds: estudianteIds.map(Number) });
      await this.cargarAlertas();
      return null;
    } catch (error) {
      return ApiService.mensaje(error);
    }
  }

  /** Devuelve null si se registró o el mensaje de error del backend. */
  async registrarJustificacion(estudianteId: string, fecha: string, motivo: string, archivoNombre?: string, cursoId?: string, detalle?: string): Promise<string | null> {
    if (!estudianteId || !motivo.trim()) return 'Selecciona un estudiante e ingresa el motivo.';
    try {
      const creada = await this.api.post<JustificacionApi>('/justificaciones', {
        estudianteId: Number(estudianteId), fecha, motivo: motivo.trim(), archivoNombre, detalle,
        cursoId: cursoId ? Number(cursoId) : undefined
      });
      this.justificaciones = [aJustificacion(creada), ...this.justificaciones];
      this.cambio();
      return null;
    } catch (error) {
      return ApiService.mensaje(error);
    }
  }

  /**
   * Aprobar marca el día como justificado (salvo que el estudiante estuviera presente).
   * Observar devuelve el día a su estado previo; si no había uno válido, queda como falta. (Regla aplicada en el backend.)
   */
  async revisarJustificacion(justificacion: Justificacion, estado: EstadoJustificacion, observacion?: string): Promise<string | null> {
    try {
      const revisada = aJustificacion(await this.api.patch<JustificacionApi>(`/justificaciones/${justificacion.id}/revision`, { estado, observacion }));
      this.justificaciones = this.justificaciones.map(item => item.id === revisada.id ? revisada : item);
      const dia = await this.api.get<{ fecha: string; estado: EstadoAsistencia }[]>(
        `/estudiantes/${justificacion.estudianteId}/asistencia`, { desde: justificacion.fecha, hasta: justificacion.fecha });
      this.fijar(this.clave(justificacion.estudianteId, justificacion.fecha), dia[0]?.estado ?? 'pendiente');
      this.programarAlertas();
      return null;
    } catch (error) {
      return ApiService.mensaje(error);
    }
  }

  /** Datos del colegio e identidad visual en una sola operación (PUT /institucion). */
  async guardarConfiguracion(datos: { nombre: string; codigoModular: string; direccion: string; colorPrincipal: string; colorSecundario: string; logo: string }): Promise<string | null> {
    try {
      const i = await this.api.put<InstitucionApi>('/institucion', datos);
      this.nombreInstitucion = i.nombre;
      this.direccionInstitucion = i.direccion;
      this.codigoModular = i.codigoModular;
      this.tema.establecer({ colorPrincipal: i.colorPrincipal, colorSecundario: i.colorSecundario, logo: i.logo ?? '' });
      this.cambio();
      return null;
    } catch (error) {
      return ApiService.mensaje(error);
    }
  }

  /** Matrícula manual desde el módulo del administrador. Devuelve el estudiante creado o el mensaje de error. */
  async matricularEstudiante(datos: Pick<Estudiante, 'nombre' | 'dni' | 'nivel' | 'grado' | 'seccion'> & { genero?: string }): Promise<Estudiante | string> {
    try {
      const creado = aEstudiante(await this.api.post<EstudianteApi>('/matriculas', {
        nombre: datos.nombre, dni: datos.dni.trim(), genero: datos.genero || undefined, nivel: datos.nivel, grado: datos.grado, seccion: datos.seccion
      }));
      this.estudiantes = [...this.estudiantes, creado];
      this.cambio();
      return creado;
    } catch (error) {
      return ApiService.mensaje(error);
    }
  }

  /** Cambia nivel, grado o sección conservando el historial de asistencia. Devuelve null o el error. */
  async trasladarEstudiante(estudiante: Estudiante, nivel: NivelEducativo, grado: string, seccion: string): Promise<string | null> {
    try {
      const actualizado = aEstudiante(await this.api.put<EstudianteApi>(`/matriculas/${estudiante.matriculaId}/traslado`, { nivel, grado, seccion }));
      this.estudiantes = this.estudiantes.map(e => e.id === actualizado.id ? actualizado : e);
      this.cambio();
      return null;
    } catch (error) {
      return ApiService.mensaje(error);
    }
  }

  /** Anula la matrícula: el backend la elimina o, si hay historial, la retira. Devuelve el mensaje del servidor. */
  async retirarEstudiante(estudiante: Estudiante): Promise<{ ok: boolean; mensaje: string }> {
    try {
      const r = await this.api.delete<{ mensaje: string }>(`/matriculas/${estudiante.matriculaId}`);
      this.estudiantes = this.estudiantes.filter(e => e.id !== estudiante.id);
      this.cambio();
      this.programarAlertas();
      return { ok: true, mensaje: r.mensaje };
    } catch (error) {
      return { ok: false, mensaje: ApiService.mensaje(error) };
    }
  }

  // =====================================================================

  private fijar(clave: string, estado: EstadoAsistencia): void {
    if (estado === 'pendiente') delete this.asistencia[clave];
    else this.asistencia[clave] = estado;
    this.cambio();
  }

  /** Las alertas dependen de las faltas: se recalculan en el backend poco después del último cambio. */
  private programarAlertas(): void {
    if (this.recargaAlertas) clearTimeout(this.recargaAlertas);
    this.recargaAlertas = setTimeout(() => void this.cargarAlertas().catch(() => undefined), 700);
  }

  private cambio(): void {
    this.version.update(v => v + 1);
  }

  private clave(estudianteId: string, fecha: string): string {
    return `${fecha}|${estudianteId}`;
  }

  private fechaLocal(): string {
    return this.fechaIso(new Date());
  }

  private fechaIso(fecha: Date): string {
    return `${fecha.getFullYear()}-${String(fecha.getMonth() + 1).padStart(2, '0')}-${String(fecha.getDate()).padStart(2, '0')}`;
  }
}
