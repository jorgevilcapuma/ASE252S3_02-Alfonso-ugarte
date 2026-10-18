import { FASES_PROYECTO } from '../config/proyecto';
import { ErrorApi } from '../services/error-api';

/**
 * Adaptador del modo servidor: las pantallas usan las mismas rutas que el modo local (api-local.ts) y aquí se
 * traducen a la API del backend ASE252S3_BE (8 tablas: usuarios, grados_secciones, docentes, estudiantes, cursos,
 * asistencia_estudiante, asistencia_personal y justificaciones).
 *
 * Lo que el backend no guarda se resuelve aquí:
 * - Alertas tempranas, historial del docente, resúmenes y portal del alumno: se calculan con la asistencia del backend.
 * - Datos e identidad del colegio, umbrales de alertas, comunicados y avisos a familias: se guardan en este navegador.
 * - Las fases del proyecto son fijas (config/proyecto.ts).
 */

type Metodo = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';
type Cuerpo = Record<string, unknown>;
type Parametros = Record<string, string>;
export type Backend = <T>(metodo: Metodo, ruta: string, parametros?: Parametros, cuerpo?: unknown) => Promise<T>;

type Estado = 'presente' | 'tardanza' | 'falta' | 'justificado' | 'pendiente';
type EstadoCurso = 'asistio' | 'tardanza' | 'falta' | 'justificado';

// ----- Formas del backend -----
interface AulaBe { id: number; nivel: string; grado: string; gradoEtiqueta: string; seccion: string; turno: 'Mañana' | 'Tarde'; totalEstudiantes: number; estado: string; }
interface EstudianteBe {
  id: number; nombre: string; nombres: string; apellidos: string; dni: string; genero?: string; gradoSeccionId: number; nivel: string;
  grado: string; gradoEtiqueta: string; seccion: string; turno: string; estado: string; telefono?: string; correo?: string; usuarioId?: number;
}
interface CursoBe { id: number; nombre: string; nombreConAula: string; gradoSeccionId: number; aula: string; docenteId: number; docenteNombre: string; estado: string; }
interface DocenteBe { id: number; nombres: string; apellidos: string; nombreCompleto: string; dni: string; especialidad?: string; celular?: string; correo?: string; fechaIngreso?: string; usuarioId?: number; }
interface RegistroBe { estudianteId: number; nombre: string; dni: string; estado: Estado; observacion?: string; }
interface DiaBe { registros: RegistroBe[]; }
interface MensualBe { filas: { estudianteId: number; estados: Record<string, Estado> }[]; }
interface HistorialBe { fecha: string; cursoId?: number; cursoNombre?: string; estado: Estado; observacion?: string; }
interface JustificacionBe { id: number; estudianteId: number; fecha: string; estado: string; motivo: string; observacion?: string; archivoNombre?: string; }

// ----- Datos guardados en este navegador -----
interface Reglas { horaEntrada: string; minutosTolerancia: number; faltasPreventivas: number; faltasCriticas: number; diasEdicionDocente: number; }
interface Comunicado {
  id: number; titulo: string; contenido: string; prioridad: 'Alta' | 'Media' | 'Baja'; cursoId?: number; cursoNombre?: string;
  gradoSeccionId?: number; docenteId?: number; autorId?: number; autor?: string; fechaCreacion: string;
}
interface Extra {
  institucion: { nombre: string; codigoModular: string; direccion: string; colorPrincipal: string; colorSecundario: string; logo: string };
  reglas: Reglas;
  comunicados: Comunicado[];
  avisos: { id: number; estudianteId: number; enviadoEn: string; faltas: number }[];
  siguienteId: number;
}

const EXTRA_KEY = 'ugartino-servidor-extra';
const SESION_KEY = 'ugartino-sesion';
const VENTANA = 20;
/** Los datos del backend se reutilizan durante este tiempo; cada cambio invalida lo que afecta. */
const VIGENCIA_MS = 60_000;
const NIVELES = ['Inicial', 'Primaria', 'Secundaria'];

const error = (status: number, mensaje: string, errores: string[] = []) => new ErrorApi(status, mensaje, errores.length ? errores : [mensaje]);
const iso = (d: Date) => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
const hoy = () => iso(new Date());
const ahora = () => new Date().toISOString().slice(0, 19);
const finDeSemana = (fecha: string) => [0, 6].includes(new Date(`${fecha}T12:00:00`).getDay());
const limpiar = (valor: unknown) => typeof valor === 'string' && valor.trim() ? valor.trim().replace(/\s+/g, ' ') : undefined;
const texto = (valor: unknown) => typeof valor === 'string' ? valor : '';
const minutos = (hora: string) => { const [h, m] = hora.split(':').map(Number); return h * 60 + m; };
const hora = (total: number) => `${String(Math.floor(total / 60)).padStart(2, '0')}:${String(total % 60).padStart(2, '0')}`;
const aCurso: Record<Estado, EstadoCurso> = { presente: 'asistio', tardanza: 'tardanza', falta: 'falta', justificado: 'justificado', pendiente: 'asistio' };
const deCurso: Record<EstadoCurso, Estado> = { asistio: 'presente', tardanza: 'tardanza', falta: 'falta', justificado: 'justificado' };

function diasHabiles(cantidad: number, hasta: string): string[] {
  const dias: string[] = [];
  const cursor = new Date(`${hasta}T12:00:00`);
  while (dias.length < cantidad) {
    if (![0, 6].includes(cursor.getDay())) dias.unshift(iso(cursor));
    cursor.setDate(cursor.getDate() - 1);
  }
  return dias;
}

/** Meses AAAA-MM que abarca el rango. */
function meses(desde: string, hasta: string): string[] {
  const lista: string[] = [];
  const cursor = new Date(`${desde.slice(0, 7)}-01T12:00:00`);
  while (iso(cursor).slice(0, 7) <= hasta.slice(0, 7)) {
    lista.push(iso(cursor).slice(0, 7));
    cursor.setMonth(cursor.getMonth() + 1);
  }
  return lista;
}

function mesAnterior(mes: string): string {
  const d = new Date(`${mes}-01T12:00:00`);
  d.setMonth(d.getMonth() - 1);
  return iso(d).slice(0, 7);
}

/** "MAT-1A" a partir de "Matemática" y el aula. */
function codigoCurso(nombre: string, aula?: AulaBe): string {
  const sigla = nombre.normalize('NFD').replace(/[̀-ͯ]/g, '').replace(/[^A-Za-z]/g, '').slice(0, 3).toUpperCase();
  return aula ? `${sigla}-${aula.grado.replace(/\D/g, '') || aula.grado.slice(0, 1)}${aula.seccion}` : sigla;
}

export class ApiServidor {
  private readonly cache = new Map<string, { hasta: number; valor: Promise<unknown> }>();
  /** Aula de cada estudiante visto en el padrón: para saber qué asistencia invalidar tras un cambio. */
  private readonly aulaDe = new Map<number, number>();

  constructor(private readonly backend: Backend) {}

  async atender(metodo: string, ruta: string, parametros: Parametros = {}, cuerpo: Cuerpo = {}): Promise<unknown> {
    const [camino, consulta] = ruta.split('?');
    const params = { ...Object.fromEntries(new URLSearchParams(consulta ?? '')), ...parametros };
    for (const [verbo, patron, manejador] of this.rutas()) {
      const coincidencia = metodo === verbo ? camino.match(patron) : null;
      if (coincidencia) return manejador(coincidencia.slice(1), params, cuerpo ?? {});
    }
    throw error(404, 'La ruta solicitada no existe.');
  }

  private rutas(): [string, RegExp, (m: string[], p: Parametros, c: Cuerpo) => Promise<unknown> | unknown][] {
    const id = '(\\d+)';
    const fecha = '(\\d{4}-\\d{2}-\\d{2})';
    return [
      ['POST', /^\/auth\/login$/, (_, __, c) => {
        this.cache.clear();   // nada de la sesión anterior se reutiliza
        return this.backend('POST', '/auth/login', undefined, c);
      }],
      ['GET', /^\/institucion$/, () => this.institucion()],
      ['PUT', /^\/institucion$/, (_, __, c) => this.guardarInstitucion(c)],
      ['PUT', /^\/institucion\/reglas$/, (_, __, c) => this.guardarReglas(c)],
      ['GET', /^\/estructura$/, () => this.estructura()],
      ['GET', /^\/fases$/, () => FASES_PROYECTO.map(f => ({ ...f }))],
      ['GET', /^\/matriculas$/, async () => ({ contenido: (await this.estudiantes()).map(e => this.estudianteApi(e)), totalPaginas: 1 })],
      ['GET', /^\/asistencia$/, (_, p) => this.rango(p)],
      ['PUT', new RegExp(`^/asistencia/estudiantes/${id}/${fecha}$`), ([e, f], __, c) => this.registrar(+e, f, c)],
      ['PUT', new RegExp(`^/asistencia/secciones/${id}$`), ([s], __, c) => this.registrarLote(+s, c)],
      ['GET', new RegExp(`^/estudiantes/${id}/asistencia$`), ([e], p) => this.historialDiario(+e, p)],
      ['PUT', new RegExp(`^/estudiantes/${id}/contacto$`), ([e], __, c) => this.contacto(+e, c)],
      ['GET', /^\/justificaciones$/, (_, p) => this.backend('GET', '/justificaciones', p)],
      ['POST', /^\/justificaciones$/, (_, __, c) => this.backend('POST', '/justificaciones', undefined, c)],
      ['PATCH', new RegExp(`^/justificaciones/${id}/revision$`), ([j], __, c) => this.revisar(+j, c)],
      ['GET', /^\/alertas$/, () => this.alertas()],
      ['POST', /^\/alertas\/avisos$/, (_, __, c) => this.avisar(c)],
      ['GET', /^\/asistencia-personal$/, (_, p) => this.personal('GET', '/asistencia-personal', p)],
      ['GET', /^\/asistencia-personal\/reporte$/, (_, p) => this.personal('GET', '/asistencia-personal/reporte', p)],
      ['PUT', new RegExp(`^/asistencia-personal/${id}/(entrada|salida)$`), ([d, tipo], __, c) => this.personal('PUT', `/asistencia-personal/${d}/${tipo}`, undefined, c)],
      ['DELETE', new RegExp(`^/asistencia-personal/${id}/${fecha}$`), ([d, f]) => this.backend('DELETE', `/asistencia-personal/${d}/${f}`)],
      ['GET', /^\/usuarios$/, (_, p) => this.backend('GET', '/usuarios', p)],
      ['POST', /^\/usuarios$/, (_, __, c) => this.backend('POST', '/usuarios', undefined, c)],
      ['PUT', new RegExp(`^/usuarios/${id}$`), ([u], __, c) => this.backend('PUT', `/usuarios/${u}`, undefined, c)],
      ['PATCH', new RegExp(`^/usuarios/${id}/estado$`), ([u]) => this.backend('PATCH', `/usuarios/${u}/estado`, undefined, {})],
      ['DELETE', new RegExp(`^/usuarios/${id}$`), ([u]) => this.backend('DELETE', `/usuarios/${u}`)],
      // CRUD de las tablas maestras (pantallas de Gestión): pasan tal cual al backend.
      ['GET', /^\/(docentes|estudiantes|cursos|grados-secciones)$/, ([tabla], p) => this.backend('GET', `/${tabla}`, p)],
      ['POST', /^\/(docentes|estudiantes|cursos|grados-secciones)$/, ([tabla], __, c) => this.cambio(this.backend('POST', `/${tabla}`, undefined, c))],
      ['PUT', new RegExp(`^/(docentes|estudiantes|cursos|grados-secciones)/${id}$`), ([tabla, r], __, c) => this.cambio(this.backend('PUT', `/${tabla}/${r}`, undefined, c))],
      ['PATCH', new RegExp(`^/(docentes|estudiantes|cursos|grados-secciones)/${id}/(eliminar|restaurar)$`),
        ([tabla, r, accion]) => this.cambio(this.backend('PATCH', `/${tabla}/${r}/${accion}`, undefined, {}))],
      ['GET', new RegExp(`^/docentes/${id}$`), ([d]) => this.docente(+d)],
      ['GET', new RegExp(`^/docentes/${id}/cursos$`), ([d]) => this.cursosDocente(+d)],
      ['GET', new RegExp(`^/docentes/${id}/estudiantes$`), ([d]) => this.estudiantesDocente(+d)],
      ['GET', new RegExp(`^/docentes/${id}/historial$`), ([d]) => this.historialDocente(+d)],
      ['PUT', new RegExp(`^/docentes/${id}/perfil$`), () => {
        throw error(403, 'Tu ficha (nombres, teléfono) la actualiza el administrador en Docentes y personal.');
      }],
      ['GET', /^\/comunicados$/, () => this.listarComunicados()],
      ['POST', /^\/comunicados$/, (_, __, c) => this.publicar(c)],
      ['DELETE', new RegExp(`^/comunicados/${id}$`), ([c]) => this.eliminarComunicado(+c)],
      ['GET', new RegExp(`^/asignaciones/${id}/sesiones/${fecha}$`), ([a, f]) => this.sesionCurso(+a, f)],
      ['PUT', new RegExp(`^/asignaciones/${id}/sesiones/${fecha}$`), ([a, f], __, c) => this.guardarSesion(+a, f, c)],
      ['GET', new RegExp(`^/asignaciones/${id}/resumen-mensual$`), ([a], p) => this.resumenMensual(+a, p['mes'] ?? hoy().slice(0, 7))],
      ['GET', new RegExp(`^/alumnos/${id}/perfil$`), async ([e]) => this.estudianteApi(await this.backend<EstudianteBe>('GET', `/estudiantes/${e}`))],
      ['GET', new RegExp(`^/alumnos/${id}/cursos$`), ([e]) => this.cursosAlumno(+e)],
      ['GET', new RegExp(`^/alumnos/${id}/asistencia$`), ([e], p) => this.asistenciaAlumno(+e, p)],
      ['GET', new RegExp(`^/alumnos/${id}/alertas$`), ([e]) => this.alertasAlumno(+e)],
      ['GET', new RegExp(`^/alumnos/${id}/comunicaciones$`), ([e]) => this.comunicacionesAlumno(+e)]
    ];
  }

  // =====================================================================
  // Lecturas del backend con caché corta
  // =====================================================================

  private memo<T>(clave: string, cargar: () => Promise<T>): Promise<T> {
    const guardado = this.cache.get(clave);
    if (guardado && guardado.hasta > Date.now()) return guardado.valor as Promise<T>;
    const valor = cargar();
    this.cache.set(clave, { hasta: Date.now() + VIGENCIA_MS, valor });
    valor.catch(() => this.cache.delete(clave));
    return valor;
  }

  /** Un cambio en las tablas maestras afecta aulas, padrón y cursos: se descarta toda la caché. */
  private async cambio<T>(peticion: Promise<T>): Promise<T> {
    const resultado = await peticion;
    this.cache.clear();
    return resultado;
  }

  /** Borra de la caché lo que empieza con alguno de los prefijos. */
  private invalidar(...prefijos: string[]): void {
    for (const clave of [...this.cache.keys()]) {
      if (prefijos.some(p => clave.startsWith(p))) this.cache.delete(clave);
    }
  }

  /** Aulas activas. El alumno no puede listarlas en el backend: para él solo existe la suya (tomada de su ficha). */
  private aulas(): Promise<AulaBe[]> {
    return this.memo('aulas', async () => {
      const s = this.sesion;
      if (s?.perfil !== 'alumno' || !s.estudianteId) return this.backend<AulaBe[]>('GET', '/grados-secciones', { estado: 'A' });
      const e = await this.backend<EstudianteBe>('GET', `/estudiantes/${s.estudianteId}`);
      return [{ id: e.gradoSeccionId, nivel: e.nivel, grado: e.grado, gradoEtiqueta: e.gradoEtiqueta, seccion: e.seccion, turno: e.turno as AulaBe['turno'], totalEstudiantes: 0, estado: 'A' }];
    });
  }

  private async aula(id: number): Promise<AulaBe | undefined> {
    return (await this.aulas()).find(a => a.id === id);
  }

  private estudiantes(): Promise<EstudianteBe[]> {
    return this.memo('estudiantes', () => this.backend<EstudianteBe[]>('GET', '/estudiantes', { estado: 'A' }).then(lista => this.recordarAulas(lista)));
  }

  private estudiantesDeAula(aulaId: number): Promise<EstudianteBe[]> {
    return this.memo(`estudiantes|${aulaId}`,
      () => this.backend<EstudianteBe[]>('GET', '/estudiantes', { gradoSeccionId: String(aulaId), estado: 'A' }).then(lista => this.recordarAulas(lista)));
  }

  private recordarAulas(lista: EstudianteBe[]): EstudianteBe[] {
    lista.forEach(e => this.aulaDe.set(e.id, e.gradoSeccionId));
    return lista;
  }

  private curso(id: number): Promise<CursoBe> {
    return this.memo(`curso|${id}`, () => this.backend<CursoBe>('GET', `/cursos/${id}`));
  }

  /** Matriz mensual del aula: sin curso es la asistencia diaria; con curso, la de esa clase. */
  private mensual(aulaId: number, mes: string, cursoId?: number): Promise<MensualBe> {
    const params: Parametros = { mes };
    if (cursoId) params['cursoId'] = String(cursoId);
    return this.memo(`mensual|${aulaId}|${cursoId ?? 'dia'}|${mes}`,
      () => this.backend<MensualBe>('GET', `/asistencia-estudiantes/grados-secciones/${aulaId}/mensual`, params));
  }

  /** Estados registrados (sin "pendiente") de un aula y un rango: estudianteId → fecha → estado. */
  private async estadosDeAula(aulaId: number, desde: string, hasta: string, cursoId?: number): Promise<Map<number, Map<string, Estado>>> {
    const resultado = new Map<number, Map<string, Estado>>();
    const matrices = await Promise.all(meses(desde, hasta).map(m => this.mensual(aulaId, m, cursoId)));
    for (const matriz of matrices) {
      for (const fila of matriz.filas) {
        const propios = resultado.get(fila.estudianteId) ?? new Map<string, Estado>();
        for (const [f, estado] of Object.entries(fila.estados)) {
          if (estado !== 'pendiente' && f >= desde && f <= hasta) propios.set(f, estado);
        }
        resultado.set(fila.estudianteId, propios);
      }
    }
    return resultado;
  }

  /** Tras cambiar la asistencia de un estudiante se vuelve a leer la de su aula (o la de todas si no se conoce). */
  private olvidarAsistenciaDe(estudianteId: number): void {
    const aula = this.aulaDe.get(estudianteId);
    this.invalidar(aula ? `mensual|${aula}|` : 'mensual|', `historial|${estudianteId}`);
  }

  // =====================================================================
  // Conversión a las formas que usan las pantallas
  // =====================================================================

  private estudianteApi(e: EstudianteBe) {
    return {
      id: e.id, nombre: e.nombre, nombres: e.nombres, apellidos: e.apellidos, dni: e.dni, genero: e.genero, nivel: e.nivel, grado: e.grado,
      gradoEtiqueta: e.gradoEtiqueta, seccion: e.seccion, seccionId: e.gradoSeccionId, turno: e.turno, matriculaId: e.id,
      estadoMatricula: e.estado === 'A' ? 'Activa' : 'Retirada', telefono: e.telefono, correo: e.correo
    };
  }

  private async cursoApi(c: CursoBe) {
    const aula = await this.aula(c.gradoSeccionId);
    return {
      id: c.id, codigo: codigoCurso(c.nombre, aula), nombre: c.nombre, nivel: aula?.nivel ?? '', grado: aula?.grado ?? '',
      gradoEtiqueta: aula?.gradoEtiqueta ?? '', seccion: aula?.seccion ?? '', seccionId: c.gradoSeccionId, docenteId: c.docenteId,
      docente: `Prof. ${c.docenteNombre}`, aula: '', horario: 'Sin horario', horarios: [], totalAlumnos: aula?.totalEstudiantes ?? 0
    };
  }

  // =====================================================================
  // Institución, reglas y estructura
  // =====================================================================

  private leerExtra(): Extra {
    const base: Extra = {
      institucion: { nombre: 'I.E. 20191 Alfonso Ugarte', codigoModular: '0285940', direccion: 'San Vicente de Cañete, Lima, Perú', colorPrincipal: '#9b1b1f', colorSecundario: '#fdc425', logo: '' },
      reglas: { horaEntrada: '07:45', minutosTolerancia: 10, faltasPreventivas: 2, faltasCriticas: 3, diasEdicionDocente: 5 },
      comunicados: [], avisos: [], siguienteId: 1
    };
    try {
      const guardado = JSON.parse(localStorage.getItem(EXTRA_KEY) ?? 'null') as Partial<Extra> | null;
      return guardado ? { ...base, ...guardado, institucion: { ...base.institucion, ...guardado.institucion }, reglas: { ...base.reglas, ...guardado.reglas } } : base;
    } catch {
      return base;
    }
  }

  private guardarExtra(extra: Extra): void {
    try {
      localStorage.setItem(EXTRA_KEY, JSON.stringify(extra));
    } catch {
      throw error(507, 'El navegador no tiene espacio para guardar este cambio (prueba con un logotipo más liviano).');
    }
  }

  private get sesion(): { perfil: string; usuarioId?: string; estudianteId?: string; docenteId?: string; nombre?: string } | null {
    try { return JSON.parse(localStorage.getItem(SESION_KEY) ?? 'null'); } catch { return null; }
  }

  private institucionApi(extra: Extra) {
    const r = extra.reglas;
    return { ...extra.institucion, reglas: { ...r, limitePuntualidad: hora(minutos(r.horaEntrada) + r.minutosTolerancia) } };
  }

  /** Datos del colegio (navegador) y, con sesión de administrador, la hora de entrada y la tolerancia que aplica el servidor. */
  private async institucion() {
    const extra = this.leerExtra();
    if (this.sesion?.perfil === 'administrador') {
      try {
        const dia = await this.memo('reglas-servidor', () => this.backend<{ horaEntrada: string; minutosTolerancia: number }>('GET', '/asistencia-personal'));
        if (dia.horaEntrada !== extra.reglas.horaEntrada || dia.minutosTolerancia !== extra.reglas.minutosTolerancia) {
          extra.reglas = { ...extra.reglas, horaEntrada: dia.horaEntrada, minutosTolerancia: dia.minutosTolerancia };
          this.guardarExtra(extra);
        }
      } catch { /* sin permiso o sin conexión: se usan los valores conocidos */ }
    }
    return this.institucionApi(extra);
  }

  private guardarInstitucion(c: Cuerpo) {
    const errores: string[] = [];
    if ((limpiar(c['nombre']) ?? '').length < 5) errores.push('Ingresa el nombre completo de la institución.');
    if (!/^\d{7}$/.test(texto(c['codigoModular']))) errores.push('El código modular tiene 7 dígitos.');
    if ((limpiar(c['direccion']) ?? '').length < 5) errores.push('Ingresa la dirección de la institución.');
    if (!/^#[0-9a-f]{6}$/i.test(texto(c['colorPrincipal'])) || !/^#[0-9a-f]{6}$/i.test(texto(c['colorSecundario']))) {
      errores.push('Usa colores hexadecimales de 6 dígitos, por ejemplo #9b1b1f.');
    }
    if (errores.length) throw error(400, errores.length > 1 ? 'Revisa los datos enviados.' : errores[0], errores);
    const extra = this.leerExtra();
    extra.institucion = {
      nombre: limpiar(c['nombre'])!, codigoModular: texto(c['codigoModular']), direccion: limpiar(c['direccion'])!,
      colorPrincipal: texto(c['colorPrincipal']).toLowerCase(), colorSecundario: texto(c['colorSecundario']).toLowerCase(), logo: texto(c['logo'])
    };
    this.guardarExtra(extra);
    return this.institucionApi(extra);
  }

  /** En modo servidor solo se editan los umbrales de alertas; la hora de entrada, la tolerancia y el plazo los fija el backend. */
  private guardarReglas(c: Cuerpo) {
    const preventivas = Number(c['faltasPreventivas']), criticas = Number(c['faltasCriticas']);
    if (!Number.isInteger(preventivas) || preventivas < 1 || preventivas > 30) throw error(400, 'Las faltas para alerta preventiva van de 1 a 30.');
    if (!Number.isInteger(criticas) || criticas < 2 || criticas > 30) throw error(400, 'Las faltas para alerta crítica van de 2 a 30.');
    if (criticas <= preventivas) throw error(400, 'Las faltas para alerta crítica deben ser más que las de alerta preventiva.');
    const extra = this.leerExtra();
    extra.reglas = { ...extra.reglas, faltasPreventivas: preventivas, faltasCriticas: criticas };
    this.guardarExtra(extra);
    return this.institucionApi(extra);
  }

  private async estructura() {
    const aulas = await this.aulas();
    return NIVELES.map(nombre => {
      const delNivel = aulas.filter(a => a.nivel === nombre);
      const grados = [...new Map(delNivel.map(a => [a.grado, a.gradoEtiqueta])).entries()];
      return {
        nombre,
        grados: grados.map(([codigo, etiqueta]) => ({
          codigo, etiqueta,
          secciones: delNivel.filter(a => a.grado === codigo).map(a => ({ id: a.id, letra: a.seccion, turno: a.turno, totalEstudiantes: a.totalEstudiantes }))
        }))
      };
    }).filter(n => n.grados.length);
  }

  // =====================================================================
  // Asistencia diaria
  // =====================================================================

  /** Registros del rango para todas las aulas (o una), como los devolvía GET /asistencia. */
  private async rango(p: Parametros) {
    const desde = p['desde'], hasta = p['hasta'];
    if (!desde || !hasta) throw error(400, "Indica los parámetros 'desde' y 'hasta'.");
    const aulas = p['seccionId'] ? [Number(p['seccionId'])] : (await this.aulas()).map(a => a.id);
    const porAula = await Promise.all(aulas.map(a => this.estadosDeAula(a, desde, hasta)));
    return porAula.flatMap(estados => [...estados.entries()].flatMap(([estudianteId, dias]) =>
      [...dias.entries()].map(([fecha, estado]) => ({ estudianteId, fecha, estado }))));
  }

  private async registrar(estudianteId: number, fecha: string, c: Cuerpo) {
    const r = await this.backend<HistorialBe>('PUT', `/asistencia-estudiantes/estudiantes/${estudianteId}`, undefined, { fecha, estado: c['estado'] });
    this.olvidarAsistenciaDe(estudianteId);
    return { fecha: r.fecha, estado: r.estado };
  }

  private async registrarLote(aulaId: number, c: Cuerpo) {
    await this.backend('PUT', `/asistencia-estudiantes/grados-secciones/${aulaId}`, undefined, { fecha: c['fecha'], registros: c['registros'] });
    this.invalidar(`mensual|${aulaId}|`);
    return { fecha: c['fecha'] };
  }

  private historialCompleto(estudianteId: number, desde: string, hasta: string): Promise<HistorialBe[]> {
    return this.memo(`historial|${estudianteId}|${desde}|${hasta}`,
      () => this.backend<HistorialBe[]>('GET', `/estudiantes/${estudianteId}/asistencia`, { desde, hasta }));
  }

  private async historialDiario(estudianteId: number, p: Parametros) {
    const hasta = p['hasta'] ?? hoy();
    const desde = p['desde'] ?? `${hasta.slice(0, 4)}-01-01`;
    return (await this.historialCompleto(estudianteId, desde, hasta)).filter(r => !r.cursoId).map(r => ({ fecha: r.fecha, estado: r.estado }));
  }

  private async contacto(estudianteId: number, c: Cuerpo) {
    const e = await this.backend<EstudianteBe>('PUT', `/estudiantes/${estudianteId}/contacto`, undefined, { telefono: c['telefono'], correo: c['correo'] });
    this.invalidar('estudiantes');
    return this.estudianteApi(e);
  }

  // =====================================================================
  // Justificaciones
  // =====================================================================

  /** Al aprobar u observar cambia la asistencia del alumno: se vuelve a leer. */
  private async revisar(id: number, c: Cuerpo) {
    const revisada = await this.backend<JustificacionBe>('PATCH', `/justificaciones/${id}/revision`, undefined, c);
    this.olvidarAsistenciaDe(revisada.estudianteId);
    return revisada;
  }

  // =====================================================================
  // Alertas tempranas (calculadas con la asistencia diaria del backend)
  // =====================================================================

  private async alertas() {
    const dia = hoy();
    const ventana = diasHabiles(VENTANA, dia);
    const { faltasCriticas, faltasPreventivas } = this.leerExtra().reglas;
    const [estudiantes, registros] = await Promise.all([this.estudiantes(), this.rango({ desde: ventana[0], hasta: dia })]);
    const faltas = new Map<number, number>();
    const faltaHoy = new Set<number>();
    for (const r of registros) {
      if (r.estado !== 'falta') continue;
      faltas.set(r.estudianteId, (faltas.get(r.estudianteId) ?? 0) + 1);
      if (r.fecha === dia) faltaHoy.add(r.estudianteId);
    }
    const avisos = this.leerExtra().avisos;
    const lista = estudiantes
      .map(e => {
        const total = faltas.get(e.id) ?? 0;
        const enviados = avisos.filter(a => a.estudianteId === e.id).map(a => a.enviadoEn).sort();
        return {
          estudiante: this.estudianteApi(e), faltas: total, faltaHoy: faltaHoy.has(e.id),
          nivel: total >= faltasCriticas ? 'critica' : total >= faltasPreventivas ? 'preventiva' : 'hoy',
          ultimoAviso: enviados[enviados.length - 1]
        };
      })
      .filter(a => a.faltas >= faltasPreventivas || a.faltaHoy)
      .sort((a, b) => b.faltas - a.faltas || a.estudiante.nombre.localeCompare(b.estudiante.nombre));
    return {
      parametros: { ventanaDias: VENTANA, faltasCriticas, faltasPreventivas, desde: ventana[0], hasta: dia },
      criticas: lista.filter(a => a.nivel === 'critica').length, preventivas: lista.filter(a => a.nivel === 'preventiva').length,
      faltasHoy: lista.filter(a => a.faltaHoy).length, alertas: lista
    };
  }

  /** Aviso de seguimiento a la familia (queda registrado en este navegador). */
  private async avisar(c: Cuerpo) {
    const ids = [...new Set((c['estudianteIds'] as number[] | undefined) ?? [])];
    if (!ids.length) throw error(400, 'Selecciona al menos un estudiante para avisar a su familia.');
    const resumen = await this.alertas();
    const extra = this.leerExtra();
    const enviados = ids.map(estudianteId => {
      const aviso = { id: extra.siguienteId++, estudianteId, enviadoEn: ahora(), faltas: resumen.alertas.find(a => a.estudiante.id === estudianteId)?.faltas ?? 0 };
      extra.avisos.push(aviso);
      return aviso;
    });
    this.guardarExtra(extra);
    return enviados;
  }

  // =====================================================================
  // Asistencia del personal (el backend identifica a cada persona por docenteId)
  // =====================================================================

  private async personal(metodo: Metodo, ruta: string, params?: Parametros, cuerpo?: Cuerpo) {
    const conId = (fila: Record<string, unknown>) => ({ ...fila, personalId: fila['docenteId'] });
    const r = await this.backend<Record<string, unknown>>(metodo, ruta, params, cuerpo);
    return Array.isArray(r['filas']) ? { ...r, filas: (r['filas'] as Record<string, unknown>[]).map(conId) } : conId(r);
  }

  // =====================================================================
  // Panel del docente
  // =====================================================================

  private async docente(id: number) {
    const d = await this.backend<DocenteBe>('GET', `/docentes/${id}`);
    return {
      id: d.id, usuarioId: d.usuarioId, nombres: d.nombres, apellidos: d.apellidos, dni: d.dni, telefono: d.celular, especialidad: d.especialidad,
      fechaIngreso: d.fechaIngreso, nombreCompleto: `${d.nombres} ${d.apellidos}`, email: d.correo
    };
  }

  private cursosBe(docenteId: number): Promise<CursoBe[]> {
    return this.memo(`cursos-docente|${docenteId}`, () => this.backend<CursoBe[]>('GET', `/docentes/${docenteId}/cursos`));
  }

  private async cursosDocente(docenteId: number) {
    return Promise.all((await this.cursosBe(docenteId)).map(c => this.cursoApi(c)));
  }

  /** Asistencia por curso del mes actual y el anterior: base de los porcentajes y del historial del docente. */
  private async asistenciaCursos(docenteId: number) {
    const mes = hoy().slice(0, 7);
    const desde = `${mesAnterior(mes)}-01`;
    const cursos = await this.cursosBe(docenteId);
    return Promise.all(cursos.map(async c => ({
      curso: c,
      aula: await this.aula(c.gradoSeccionId),
      alumnos: await this.estudiantesDeAula(c.gradoSeccionId),
      estados: await this.estadosDeAula(c.gradoSeccionId, desde, hoy(), c.id)
    })));
  }

  private async estudiantesDocente(docenteId: number) {
    return (await this.asistenciaCursos(docenteId)).flatMap(({ curso, aula, alumnos, estados }) => alumnos.map(e => {
      const propios = [...(estados.get(e.id)?.values() ?? [])];
      const porcentaje = propios.length ? Math.round(propios.filter(x => x !== 'falta').length * 100 / propios.length) : 100;
      const seccion = aula ? `${aula.gradoEtiqueta} ${aula.seccion}` : '';
      return {
        id: e.id, dni: e.dni, nombres: e.nombres, apellidos: e.apellidos, genero: e.genero, nivel: e.nivel, gradoSeccion: seccion,
        cursoId: curso.id, curso: curso.nombre, cursoNombre: `${curso.nombre} (${seccion})`, sesiones: propios.length, porcentajeAsistencia: porcentaje,
        estadoGeneral: porcentaje >= 80 ? 'Normal' : porcentaje >= 68 ? 'En Riesgo' : 'Crítico'
      };
    }));
  }

  private async historialDocente(docenteId: number) {
    const sesiones = (await this.asistenciaCursos(docenteId)).flatMap(({ curso, aula, estados }) => {
      const porFecha = new Map<string, Estado[]>();
      for (const dias of estados.values()) {
        for (const [fecha, estado] of dias) porFecha.set(fecha, [...(porFecha.get(fecha) ?? []), estado]);
      }
      return [...porFecha.entries()].map(([fecha, lista]) => {
        const contar = (e: Estado) => lista.filter(x => x === e).length;
        return {
          id: `${curso.id}-${fecha}`, fecha, cursoId: curso.id, cursoNombre: curso.nombre, gradoSeccion: aula ? `${aula.gradoEtiqueta} ${aula.seccion}` : '',
          asistieron: contar('presente'), tardanzas: contar('tardanza'), faltas: contar('falta'), justificados: contar('justificado'), totalAlumnos: lista.length
        };
      });
    });
    return sesiones.sort((a, b) => b.fecha.localeCompare(a.fecha) || a.cursoNombre.localeCompare(b.cursoNombre));
  }

  /** Lista para tomar asistencia de un curso: sin registro previo, todos aparecen como "asistio". */
  private async sesionCurso(cursoId: number, fecha: string) {
    const curso = await this.curso(cursoId);
    const [dia, alumnos] = await Promise.all([
      this.backend<DiaBe>('GET', `/asistencia-estudiantes/grados-secciones/${curso.gradoSeccionId}`, { cursoId: String(cursoId), fecha }),
      this.estudiantesDeAula(curso.gradoSeccionId)
    ]);
    const porId = new Map(alumnos.map(e => [e.id, e]));
    return {
      yaRegistrado: dia.registros.some(r => r.estado !== 'pendiente'),
      filas: dia.registros.map(r => ({
        alumnoId: r.estudianteId, nombres: porId.get(r.estudianteId)?.nombres ?? r.nombre, apellidos: porId.get(r.estudianteId)?.apellidos ?? '',
        dni: r.dni, estado: aCurso[r.estado], observacion: r.observacion ?? ''
      }))
    };
  }

  private async guardarSesion(cursoId: number, fecha: string, c: Cuerpo) {
    const curso = await this.curso(cursoId);
    const registros = ((c['registros'] as { estudianteId: number; estado: EstadoCurso; observacion?: string }[] | undefined) ?? [])
      .map(r => ({ estudianteId: r.estudianteId, estado: deCurso[r.estado] ?? r.estado, observacion: limpiar(r.observacion) }));
    if (!registros.length) throw error(400, 'Envía la asistencia de al menos un alumno.');
    await this.backend('PUT', `/asistencia-estudiantes/grados-secciones/${curso.gradoSeccionId}`, undefined, { fecha, cursoId, registros });
    this.invalidar(`mensual|${curso.gradoSeccionId}|`, 'historial|');
    return this.sesionCurso(cursoId, fecha);
  }

  private async resumenMensual(cursoId: number, mes: string) {
    const curso = await this.curso(cursoId);
    const [alumnos, estados] = await Promise.all([
      this.estudiantesDeAula(curso.gradoSeccionId),
      this.estadosDeAula(curso.gradoSeccionId, `${mes}-01`, `${mes}-31`, cursoId)
    ]);
    return alumnos.map(e => {
      const lista = [...(estados.get(e.id)?.values() ?? [])];
      const faltas = lista.filter(x => x === 'falta').length;
      return {
        alumnoId: e.id, nombres: e.nombres, apellidos: e.apellidos, dni: e.dni, sesiones: lista.length, asistencias: lista.length - faltas,
        tardanzas: lista.filter(x => x === 'tardanza').length, faltas, porcentaje: lista.length ? Math.round((lista.length - faltas) * 100 / lista.length) : 100
      };
    });
  }

  // =====================================================================
  // Portal del alumno
  // =====================================================================

  private cursosDeAlumno(): Promise<CursoBe[]> {
    return this.memo('cursos-alumno', () => this.backend<CursoBe[]>('GET', '/cursos'));
  }

  private async cursosAlumno(_estudianteId: number) {
    return Promise.all((await this.cursosDeAlumno()).map(c => this.cursoApi(c)));
  }

  /** Registros por curso y, en los días sin clase registrada, el registro diario del aula (más reciente primero). */
  private async asistenciaAlumno(estudianteId: number, p: Parametros) {
    const dia = hoy();
    const hasta = p['hasta'] && p['hasta'] < dia ? p['hasta'] : dia;
    const inicio = new Date(`${hasta}T12:00:00`);
    inicio.setDate(inicio.getDate() - 120);
    const desde = p['desde'] ?? iso(inicio);
    const cursoId = p['cursoId'] ? Number(p['cursoId']) : undefined;
    const [historial, cursos] = await Promise.all([this.historialCompleto(estudianteId, desde, hasta), this.cursosDeAlumno().catch(() => [] as CursoBe[])]);
    const docente = new Map(cursos.map(c => [c.id, `Prof. ${c.docenteNombre}`]));
    const dias: { fecha: string; estado: string; cursoId?: number; cursoNombre?: string; docente?: string; observacion?: string }[] = [];
    const conCurso = new Set<string>();
    for (const r of historial) {
      if (!r.cursoId || (cursoId && r.cursoId !== cursoId)) continue;
      conCurso.add(r.fecha);
      dias.push({ fecha: r.fecha, estado: r.estado, cursoId: r.cursoId, cursoNombre: r.cursoNombre, docente: docente.get(r.cursoId), observacion: r.observacion });
    }
    if (!cursoId) {
      for (const r of historial) {
        if (!r.cursoId && !conCurso.has(r.fecha)) dias.push({ fecha: r.fecha, estado: r.estado, observacion: r.observacion });
      }
      if (!dias.some(d => d.fecha === dia) && !finDeSemana(dia) && hasta >= dia) dias.push({ fecha: dia, estado: 'pendiente' });
    }
    dias.sort((a, b) => b.fecha.localeCompare(a.fecha) || (a.cursoNombre ?? '').localeCompare(b.cursoNombre ?? ''));
    const limite = p['limite'] ? Number(p['limite']) : p['desde'] ? Number.MAX_SAFE_INTEGER : 20;
    return dias.slice(0, limite);
  }

  private async alertasAlumno(estudianteId: number) {
    const dia = hoy();
    const [diario, justificaciones, conCurso] = await Promise.all([
      this.historialDiario(estudianteId, { desde: `${dia.slice(0, 4)}-01-01`, hasta: dia }),
      this.backend<JustificacionBe[]>('GET', '/justificaciones'),
      this.asistenciaAlumno(estudianteId, {})
    ]);
    const lista: { id: string; fecha: string; titulo: string; detalle: string; tipo: string; leido: boolean }[] = [];
    const estadoHoy = diario.find(d => d.fecha === dia)?.estado;
    if (estadoHoy === 'falta') lista.push({ id: 'falta-hoy', fecha: dia, titulo: 'Falta registrada', detalle: 'Revisa el registro de hoy y comunícate con el colegio si necesitas aclararlo.', tipo: 'asistencia', leido: false });
    if (estadoHoy === 'tardanza') lista.push({ id: 'tardanza-hoy', fecha: dia, titulo: 'Tardanza registrada', detalle: 'Se registró una tardanza en tu asistencia de hoy.', tipo: 'asistencia', leido: false });
    const contados = conCurso.filter(d => d.estado !== 'pendiente');
    const porcentaje = contados.length ? Math.round(contados.filter(d => d.estado !== 'falta').length * 100 / contados.length) : 100;
    if (contados.length >= 5 && porcentaje < 85) {
      lista.push({ id: 'porcentaje-bajo', fecha: dia, titulo: 'Porcentaje de asistencia bajo', detalle: `Tu asistencia está en ${porcentaje}%. Revisa tu historial y conversa con el colegio.`, tipo: 'asistencia', leido: false });
    }
    this.leerExtra().avisos.filter(a => a.estudianteId === estudianteId).slice(-5).forEach(a => lista.push({
      id: `aviso-${a.id}`, fecha: a.enviadoEn.slice(0, 10), titulo: 'Aviso enviado a tu familia',
      detalle: `El colegio comunicó a tu familia ${a.faltas} falta(s) acumulada(s).`, tipo: 'asistencia', leido: false
    }));
    justificaciones.filter(j => j.estudianteId === estudianteId && j.estado !== 'Pendiente').slice(0, 10).forEach(j => lista.push({
      id: `justificacion-${j.id}`, fecha: j.fecha, titulo: `Justificación ${j.estado.toLowerCase()}`,
      detalle: j.observacion ?? `${j.motivo}${j.archivoNombre ? ` · Sustento: ${j.archivoNombre}` : ''}`, tipo: 'justificacion', leido: false
    }));
    return lista.sort((a, b) => b.fecha.localeCompare(a.fecha));
  }

  // =====================================================================
  // Comunicados (el backend no tiene tabla de comunicados: se guardan en este navegador)
  // =====================================================================

  private comunicadoApi(c: Comunicado) {
    return {
      id: c.id, titulo: c.titulo, contenido: c.contenido, cursoId: c.cursoId, prioridad: c.prioridad, fechaCreacion: c.fechaCreacion,
      autorId: c.autorId, cursoNombre: c.cursoNombre ?? 'Todos los cursos', autor: c.autor
    };
  }

  private ordenar(lista: Comunicado[]): Comunicado[] {
    return [...lista].sort((a, b) => b.fechaCreacion.localeCompare(a.fechaCreacion) || b.id - a.id);
  }

  private async listarComunicados() {
    const s = this.sesion;
    let lista = this.leerExtra().comunicados;
    if (s?.perfil === 'alumno' && s.estudianteId) {
      const aula = (await this.backend<EstudianteBe>('GET', `/estudiantes/${s.estudianteId}`)).gradoSeccionId;
      lista = lista.filter(c => !c.gradoSeccionId || c.gradoSeccionId === aula);
    } else if (s?.perfil === 'docente') {
      lista = lista.filter(c => String(c.autorId) === s.usuarioId || String(c.docenteId) === s.docenteId);
    }
    return this.ordenar(lista).map(c => this.comunicadoApi(c));
  }

  private async publicar(c: Cuerpo) {
    const titulo = limpiar(c['titulo']), contenido = texto(c['contenido']).trim();
    if (!titulo || !contenido) throw error(400, 'Ingresa el título y el contenido del comunicado.');
    const prioridad = texto(c['prioridad']);
    if (!['Alta', 'Media', 'Baja'].includes(prioridad)) throw error(400, `Valor no válido para prioridad: '${prioridad}'. Valores permitidos: Alta, Media, Baja.`);
    const s = this.sesion;
    let curso: CursoBe | undefined;
    if (c['cursoId']) {
      curso = await this.curso(Number(c['cursoId']));
      if (s?.perfil === 'docente' && String(curso.docenteId) !== s.docenteId) throw error(403, 'Solo puedes publicar comunicados en tus cursos.');
    }
    const aula = curso ? await this.aula(curso.gradoSeccionId) : undefined;
    const extra = this.leerExtra();
    const comunicado: Comunicado = {
      id: extra.siguienteId++, titulo, contenido, prioridad: prioridad as Comunicado['prioridad'], cursoId: curso?.id,
      cursoNombre: curso ? `${curso.nombre} - ${aula ? `${aula.gradoEtiqueta} ${aula.seccion}` : curso.aula}` : undefined,
      gradoSeccionId: curso?.gradoSeccionId, docenteId: curso?.docenteId, autorId: Number(s?.usuarioId) || undefined, autor: s?.nombre, fechaCreacion: hoy()
    };
    extra.comunicados.push(comunicado);
    this.guardarExtra(extra);
    return this.comunicadoApi(comunicado);
  }

  private eliminarComunicado(id: number) {
    const extra = this.leerExtra();
    const c = extra.comunicados.find(x => x.id === id);
    if (!c) throw error(404, `No existe el comunicado con id ${id}.`);
    const s = this.sesion;
    if (s?.perfil !== 'administrador' && String(c.autorId) !== s?.usuarioId) throw error(403, 'Solo el autor o un administrador puede modificar este comunicado.');
    extra.comunicados = extra.comunicados.filter(x => x !== c);
    this.guardarExtra(extra);
    return null;
  }

  private async comunicacionesAlumno(estudianteId: number) {
    const aula = (await this.backend<EstudianteBe>('GET', `/estudiantes/${estudianteId}`)).gradoSeccionId;
    return this.ordenar(this.leerExtra().comunicados.filter(c => !c.gradoSeccionId || c.gradoSeccionId === aula)).map(c => ({
      id: `comunicado-${c.id}`, fecha: c.fechaCreacion, titulo: c.prioridad === 'Alta' ? `Importante: ${c.titulo}` : c.titulo,
      detalle: `${c.contenido} · ${c.cursoNombre ?? 'Todos los cursos'}`, tipo: 'comunicado', leido: false
    }));
  }
}
