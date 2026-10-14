import { ErrorApi } from '../services/error-api';

/**
 * API simulada del modo local (sin backend). Atiende las mismas rutas que el backend Spring Boot,
 * con los mismos datos de demostración y las mismas reglas principales, y guarda todo en localStorage.
 */

type Estado = 'presente' | 'tardanza' | 'falta' | 'justificado';
type EstadoCurso = 'asistio' | 'tardanza' | 'falta' | 'justificado';
type Perfil = 'administrador' | 'docente' | 'alumno';
type Cuerpo = Record<string, unknown>;
type Parametros = Record<string, string>;

interface Seccion { id: number; nivel: string; grado: string; etiqueta: string; letra: string; turno: 'Mañana' | 'Tarde'; }
interface Usuario { id: number; nombre: string; correo: string; perfil: Perfil; estado: 'Activo' | 'Inactivo'; creado: string; clave: string; }
interface Docente {
  id: number; usuarioId?: number; nombres: string; apellidos: string; dni: string; telefono?: string; especialidad?: string;
  colegiatura?: string; codigoPlaza?: string; condicion?: string; nivelEducativo?: string; fechaIngreso?: string;
}
interface Estudiante {
  id: number; matriculaId: number; usuarioId?: number; nombres: string; apellidos: string; dni: string; genero?: string;
  telefono?: string; correo?: string; seccionId: number; estadoMatricula: 'Activa' | 'Retirada';
}
interface Horario { dia: string; inicio: string; fin: string; }
interface Asignacion { id: number; codigo: string; curso: string; seccionId: number; docenteId: number; aula: string; horarios: Horario[]; }
interface Sesion { id: number; asignacionId: number; fecha: string; registros: Record<string, { estado: EstadoCurso; observacion: string }>; }
interface Justificacion {
  id: number; estudianteId: number; asignacionId?: number; fecha: string; motivo: string; detalle?: string; archivoNombre?: string;
  estado: 'Pendiente' | 'Aprobada' | 'Observada'; estadoPrevio: Estado | 'pendiente'; observacion?: string; fechaSolicitud: string;
  fechaRevision?: string; revisadoPor?: string;
}
interface Comunicado { id: number; titulo: string; contenido: string; prioridad: 'Alta' | 'Media' | 'Baja'; asignacionId?: number; autorId?: number; fechaCreacion: string; }
interface Bd {
  version: number;
  siguienteId: number;
  institucion: { nombre: string; codigoModular: string; direccion: string; colorPrincipal: string; colorSecundario: string; logo: string };
  niveles: { nombre: string; grados: { codigo: string; etiqueta: string }[] }[];
  secciones: Seccion[];
  fases: { semana: string; titulo: string; actividad: string; producto: string; estado: string }[];
  usuarios: Usuario[];
  docentes: Docente[];
  estudiantes: Estudiante[];
  asignaciones: Asignacion[];
  asistencia: Record<string, Estado>;
  sesiones: Sesion[];
  justificaciones: Justificacion[];
  avisos: { id: number; estudianteId: number; enviadoEn: string; faltas: number }[];
  comunicados: Comunicado[];
}

const BD_KEY = 'ugartino-local-bd';
const SESION_KEY = 'ugartino-sesion';
const VERSION = 1;
const VENTANA = 20;
const CRITICAS = 3;
const PREVENTIVAS = 2;
const DIAS = ['Domingo', 'Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado'];
const ABREVIATURA: Record<string, string> = { Lunes: 'Lun', Martes: 'Mar', 'Miércoles': 'Mié', Jueves: 'Jue', Viernes: 'Vie' };

// ---------------------------------------------------------------------
// Utilidades
// ---------------------------------------------------------------------

const error = (status: number, mensaje: string, errores: string[] = []) => new ErrorApi(status, mensaje, errores.length ? errores : [mensaje]);
const iso = (d: Date) => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
const hoy = () => iso(new Date());
const ahora = () => new Date().toISOString().slice(0, 19);
const finDeSemana = (fecha: string) => [0, 6].includes(new Date(`${fecha}T12:00:00`).getDay());
const diaSemana = (fecha: string) => DIAS[new Date(`${fecha}T12:00:00`).getDay()];
const clave = (texto: string) => texto.trim().normalize('NFD').replace(/[̀-ͯ]/g, '').replace(/\s+/g, ' ').toLowerCase();
const limpiar = (texto: unknown) => typeof texto === 'string' && texto.trim() ? texto.trim().replace(/\s+/g, ' ') : undefined;
const texto = (valor: unknown) => typeof valor === 'string' ? valor : '';

function diasHabiles(cantidad: number, hasta: string): string[] {
  const dias: string[] = [];
  const cursor = new Date(`${hasta}T12:00:00`);
  while (dias.length < cantidad) {
    if (![0, 6].includes(cursor.getDay())) dias.unshift(iso(cursor));
    cursor.setDate(cursor.getDate() - 1);
  }
  return dias;
}

function separarNombre(nombre: string): [string, string] | null {
  const partes = nombre.trim().split(/\s+/).filter(Boolean);
  if (partes.length < 2) return null;
  const corte = partes.length >= 3 ? partes.length - 2 : 1;
  return [partes.slice(0, corte).join(' '), partes.slice(corte).join(' ')];
}

/** Mismo hash que el backend (mezcla murmur3) para repartir faltas de forma estable. */
function hash(cadena: string): number {
  let h = 7;
  for (const caracter of cadena) h = (Math.imul(h, 31) + caracter.charCodeAt(0)) | 0;
  h = Math.imul(h ^ (h >>> 16), 0x85ebca6b);
  h = Math.imul(h ^ (h >>> 13), 0xc2b2ae35);
  return (h ^ (h >>> 16)) >>> 0;
}

function etiquetaGrado(nivel: string, codigo: string): string {
  if (nivel === 'Inicial') return codigo;
  const ordinal = ({ '1': '1ro', '2': '2do', '3': '3ro', '4': '4to', '5': '5to', '6': '6to' } as Record<string, string>)[codigo] ?? `${codigo}°`;
  return `${ordinal} ${nivel === 'Secundaria' ? 'año' : 'grado'}`;
}

function horarioEnTexto(horarios: Horario[]): string {
  if (!horarios.length) return 'Sin horario';
  const porHoras = new Map<string, string[]>();
  for (const h of ordenarHorarios(horarios)) {
    const rango = `${h.inicio} - ${h.fin}`;
    porHoras.set(rango, [...(porHoras.get(rango) ?? []), h.dia]);
  }
  return [...porHoras.entries()].map(([rango, dias]) =>
    `${dias.length === 1 ? dias[0] : dias.map(d => ABREVIATURA[d] ?? d).join(' - ')} ${rango}`).join(' · ');
}

function ordenarHorarios(horarios: Horario[]): Horario[] {
  return [...horarios].sort((a, b) => DIAS.indexOf(a.dia) - DIAS.indexOf(b.dia) || a.inicio.localeCompare(b.inicio));
}

// ---------------------------------------------------------------------
// API local
// ---------------------------------------------------------------------

export class ApiLocal {
  private bd: Bd = this.cargar();

  /** Vuelve a los datos de demostración iniciales. */
  reiniciar(): void {
    this.bd = this.sembrar();
    this.guardar();
  }

  async atender(metodo: string, ruta: string, parametros: Parametros = {}, cuerpo: Cuerpo = {}): Promise<unknown> {
    const [camino, consulta] = ruta.split('?');
    const params = { ...Object.fromEntries(new URLSearchParams(consulta ?? '')), ...parametros };
    for (const [verbo, patron, manejador] of this.rutas()) {
      const coincidencia = metodo === verbo ? camino.match(patron) : null;
      if (coincidencia) {
        const resultado = manejador(coincidencia.slice(1), params, cuerpo ?? {});
        if (metodo !== 'GET') this.guardar();
        return structuredClone(resultado);
      }
    }
    throw error(404, 'La ruta solicitada no existe.');
  }

  private rutas(): [string, RegExp, (m: string[], p: Parametros, c: Cuerpo) => unknown][] {
    const id = '(\\d+)';
    const fecha = '(\\d{4}-\\d{2}-\\d{2})';
    return [
      ['POST', /^\/auth\/login$/, (_, __, c) => this.login(c)],
      ['GET', /^\/institucion$/, () => this.bd.institucion],
      ['PUT', /^\/institucion$/, (_, __, c) => this.guardarInstitucion(c)],
      ['GET', /^\/estructura$/, () => this.estructura()],
      ['GET', /^\/fases$/, () => this.bd.fases],
      ['GET', /^\/matriculas$/, () => ({ contenido: this.activos().map(e => this.estudianteApi(e)), totalPaginas: 1 })],
      ['POST', /^\/matriculas$/, (_, __, c) => this.matricular(c)],
      ['PUT', new RegExp(`^/matriculas/${id}/traslado$`), ([m], __, c) => this.trasladar(+m, c)],
      ['DELETE', new RegExp(`^/matriculas/${id}$`), ([m]) => this.anular(+m)],
      ['GET', /^\/asistencia$/, (_, p) => this.rango(p)],
      ['PUT', new RegExp(`^/asistencia/estudiantes/${id}/${fecha}$`), ([e, f], __, c) => this.registrar(+e, f, texto(c['estado']))],
      ['PUT', new RegExp(`^/asistencia/secciones/${id}$`), ([s], __, c) => this.registrarLote(+s, c)],
      ['GET', new RegExp(`^/estudiantes/${id}/asistencia$`), ([e], p) => this.historialDiario(+e, p)],
      ['PUT', new RegExp(`^/estudiantes/${id}/contacto$`), ([e], __, c) => this.contacto(+e, c)],
      ['GET', /^\/justificaciones$/, (_, p) => this.listarJustificaciones(p)],
      ['POST', /^\/justificaciones$/, (_, __, c) => this.solicitarJustificacion(c)],
      ['PATCH', new RegExp(`^/justificaciones/${id}/revision$`), ([j], __, c) => this.revisar(+j, c)],
      ['GET', /^\/alertas$/, () => this.alertas()],
      ['POST', /^\/alertas\/avisos$/, (_, __, c) => this.avisar(c)],
      ['GET', /^\/usuarios$/, () => this.bd.usuarios.map(u => this.usuarioApi(u))],
      ['POST', /^\/usuarios$/, (_, __, c) => this.guardarUsuario(c)],
      ['PUT', new RegExp(`^/usuarios/${id}$`), ([u], __, c) => this.guardarUsuario(c, +u)],
      ['PATCH', new RegExp(`^/usuarios/${id}/estado$`), ([u]) => this.alternarUsuario(+u)],
      ['DELETE', new RegExp(`^/usuarios/${id}$`), ([u]) => this.eliminarUsuario(+u)],
      ['GET', new RegExp(`^/docentes/${id}$`), ([d]) => this.docenteApi(this.docente(+d))],
      ['GET', new RegExp(`^/docentes/${id}/cursos$`), ([d]) => this.bd.asignaciones.filter(a => a.docenteId === +d).map(a => this.cursoApi(a))],
      ['GET', new RegExp(`^/docentes/${id}/estudiantes$`), ([d]) => this.estudiantesDocente(+d)],
      ['GET', new RegExp(`^/docentes/${id}/historial$`), ([d]) => this.historialDocente(+d)],
      ['PUT', new RegExp(`^/docentes/${id}/perfil$`), ([d], __, c) => this.perfilDocente(+d, c)],
      ['GET', /^\/comunicados$/, () => this.listarComunicados()],
      ['POST', /^\/comunicados$/, (_, __, c) => this.publicar(c)],
      ['DELETE', new RegExp(`^/comunicados/${id}$`), ([c]) => this.eliminarComunicado(+c)],
      ['GET', new RegExp(`^/asignaciones/${id}/sesiones/${fecha}$`), ([a, f]) => this.sesionCurso(+a, f)],
      ['PUT', new RegExp(`^/asignaciones/${id}/sesiones/${fecha}$`), ([a, f], __, c) => this.guardarSesion(+a, f, c)],
      ['GET', new RegExp(`^/asignaciones/${id}/resumen-mensual$`), ([a], p) => this.resumenMensual(+a, p['mes'] ?? hoy().slice(0, 7))],
      ['GET', new RegExp(`^/alumnos/${id}/perfil$`), ([e]) => this.estudianteApi(this.estudiantePropio(+e))],
      ['GET', new RegExp(`^/alumnos/${id}/cursos$`), ([e]) => this.cursosAlumno(+e)],
      ['GET', new RegExp(`^/alumnos/${id}/asistencia$`), ([e], p) => this.asistenciaAlumno(+e, p)],
      ['GET', new RegExp(`^/alumnos/${id}/alertas$`), ([e]) => this.alertasAlumno(+e)],
      ['GET', new RegExp(`^/alumnos/${id}/comunicaciones$`), ([e]) => this.comunicacionesAlumno(+e)]
    ];
  }

  // =====================================================================
  // Sesión
  // =====================================================================

  private login(c: Cuerpo) {
    const perfil = texto(c['perfil']) as Perfil;
    if (!['administrador', 'docente', 'alumno'].includes(perfil)) throw error(400, 'Selecciona tu perfil de acceso.');
    const respuesta = (u: Usuario | undefined, nombre: string, correo: string, estudianteId?: number, docenteId?: number, dni?: string) => ({
      token: 'modo-local', expiraEn: Date.now() + 10 * 3600_000,
      sesion: { usuarioId: u?.id ?? null, correo, nombre, perfil, estudianteId: estudianteId ?? null, docenteId: docenteId ?? null, dni: dni ?? null }
    });

    if (perfil === 'alumno' && limpiar(c['dni'])) {
      const nombre = limpiar(c['nombre']) ?? '';
      const e = this.bd.estudiantes.find(x => x.dni === texto(c['dni']).trim() && clave(`${x.nombres} ${x.apellidos}`) === clave(nombre));
      if (!e) throw error(401, 'No encontramos un estudiante con ese nombre y DNI. Verifica tus datos.');
      const cuenta = this.bd.usuarios.find(u => u.id === e.usuarioId);
      if (cuenta?.estado === 'Inactivo') throw error(403, 'Esta cuenta está desactivada. Comunícate con el administrador.');
      if (e.estadoMatricula !== 'Activa') throw error(403, 'No tienes una matrícula activa este año. Comunícate con el colegio.');
      return respuesta(cuenta, `${e.nombres} ${e.apellidos}`, cuenta?.correo ?? e.correo ?? '', e.id, undefined, e.dni);
    }

    const correo = texto(c['correo']).trim().toLowerCase();
    const u = this.bd.usuarios.find(x => x.correo === correo && x.clave === texto(c['contrasena']));
    if (!u) throw error(401, 'Correo o contraseña incorrectos.');
    if (u.estado === 'Inactivo') throw error(403, 'Esta cuenta está desactivada. Comunícate con el administrador.');
    if (u.perfil !== perfil) throw error(403, `Esta cuenta pertenece al perfil ${{ administrador: 'Administrador', docente: 'Docente', alumno: 'Alumno' }[u.perfil]}.`);
    const docente = this.bd.docentes.find(d => d.usuarioId === u.id);
    const estudiante = this.bd.estudiantes.find(e => e.usuarioId === u.id);
    if (perfil === 'alumno' && !estudiante) throw error(403, 'Tu cuenta no está vinculada a un estudiante. Comunícate con el administrador.');
    return respuesta(u, u.nombre, u.correo, estudiante?.id, docente?.id, docente?.dni ?? estudiante?.dni);
  }

  private get sesion(): { perfil: Perfil; usuarioId?: string; estudianteId?: string; docenteId?: string } | null {
    try { return JSON.parse(localStorage.getItem(SESION_KEY) ?? 'null'); } catch { return null; }
  }

  // =====================================================================
  // Institución y estructura
  // =====================================================================

  private guardarInstitucion(c: Cuerpo) {
    const errores: string[] = [];
    if ((limpiar(c['nombre']) ?? '').length < 5) errores.push('Ingresa el nombre completo de la institución.');
    if (!/^\d{7}$/.test(texto(c['codigoModular']))) errores.push('El código modular tiene 7 dígitos.');
    if ((limpiar(c['direccion']) ?? '').length < 5) errores.push('Ingresa la dirección de la institución.');
    if (!/^#[0-9a-f]{6}$/i.test(texto(c['colorPrincipal'])) || !/^#[0-9a-f]{6}$/i.test(texto(c['colorSecundario']))) {
      errores.push('Usa colores hexadecimales de 6 dígitos, por ejemplo #9b1b1f.');
    }
    if (errores.length) throw error(400, errores.length > 1 ? 'Revisa los datos enviados.' : errores[0], errores);
    this.bd.institucion = {
      nombre: limpiar(c['nombre'])!, codigoModular: texto(c['codigoModular']), direccion: limpiar(c['direccion'])!,
      colorPrincipal: texto(c['colorPrincipal']).toLowerCase(), colorSecundario: texto(c['colorSecundario']).toLowerCase(), logo: texto(c['logo'])
    };
    return this.bd.institucion;
  }

  private estructura() {
    return this.bd.niveles.map(n => ({
      nombre: n.nombre,
      grados: n.grados.map(g => ({
        codigo: g.codigo, etiqueta: g.etiqueta,
        secciones: this.bd.secciones.filter(s => s.nivel === n.nombre && s.grado === g.codigo)
          .map(s => ({ id: s.id, letra: s.letra, turno: s.turno, totalEstudiantes: this.activos().filter(e => e.seccionId === s.id).length }))
      }))
    }));
  }

  // =====================================================================
  // Matrículas y estudiantes
  // =====================================================================

  private activos(): Estudiante[] {
    const orden = (e: Estudiante) => {
      const s = this.seccion(e.seccionId);
      const n = this.bd.niveles.findIndex(x => x.nombre === s.nivel);
      const g = this.bd.niveles[n]?.grados.findIndex(x => x.codigo === s.grado) ?? 0;
      return `${n}|${String(g).padStart(2, '0')}|${s.letra}|${e.apellidos}|${e.nombres}`;
    };
    return this.bd.estudiantes.filter(e => e.estadoMatricula === 'Activa').sort((a, b) => orden(a).localeCompare(orden(b)));
  }

  private estudianteApi(e: Estudiante) {
    const s = this.seccion(e.seccionId);
    return {
      id: e.id, nombre: `${e.nombres} ${e.apellidos}`, nombres: e.nombres, apellidos: e.apellidos, dni: e.dni, genero: e.genero,
      nivel: s.nivel, grado: s.grado, gradoEtiqueta: s.etiqueta, seccion: s.letra, seccionId: s.id, turno: s.turno,
      matriculaId: e.matriculaId, estadoMatricula: e.estadoMatricula, telefono: e.telefono, correo: e.correo
    };
  }

  private seccionPorNombre(c: Cuerpo): Seccion {
    const s = this.bd.secciones.find(x => x.nivel === texto(c['nivel']) && x.grado === texto(c['grado']) && x.letra === texto(c['seccion']));
    if (!s) throw error(400, `Selecciona un grado y una sección válidos para el nivel ${texto(c['nivel'])}.`);
    return s;
  }

  private matricular(c: Cuerpo) {
    const errores: string[] = [];
    const partes = separarNombre(texto(c['nombre']));
    if (!/^\d{8}$/.test(texto(c['dni']))) errores.push('El DNI debe tener 8 dígitos.');
    if (!partes) errores.push('Ingresa nombres y apellidos.');
    if (c['genero'] && !['M', 'F'].includes(texto(c['genero']))) errores.push('El género debe ser M o F.');
    if (errores.length) throw error(400, errores[0], errores);
    const existente = this.bd.estudiantes.find(e => e.dni === texto(c['dni']));
    if (existente) {
      throw error(409, clave(`${existente.nombres} ${existente.apellidos}`) === clave(texto(c['nombre']))
        ? 'Ya existe un estudiante matriculado con ese DNI.'
        : `Ese DNI ya está registrado con otro nombre (${existente.nombres} ${existente.apellidos}). Verifica los datos.`);
    }
    const s = this.seccionPorNombre(c);
    const e: Estudiante = {
      id: this.nuevoId(), matriculaId: this.nuevoId(), nombres: partes![0], apellidos: partes![1], dni: texto(c['dni']),
      genero: limpiar(c['genero']), seccionId: s.id, estadoMatricula: 'Activa'
    };
    this.bd.estudiantes.push(e);
    return this.estudianteApi(e);
  }

  private porMatricula(matriculaId: number): Estudiante {
    const e = this.bd.estudiantes.find(x => x.matriculaId === matriculaId);
    if (!e) throw error(404, `No existe la matrícula con id ${matriculaId}.`);
    return e;
  }

  private trasladar(matriculaId: number, c: Cuerpo) {
    const e = this.porMatricula(matriculaId);
    if (e.estadoMatricula !== 'Activa') throw error(409, 'No se puede trasladar una matrícula retirada.');
    const destino = this.seccionPorNombre(c);
    if (destino.id === e.seccionId) throw error(400, 'El estudiante ya pertenece a esa sección.');
    e.seccionId = destino.id;
    return this.estudianteApi(e);
  }

  private anular(matriculaId: number) {
    const e = this.porMatricula(matriculaId);
    const tieneHistorial = Object.keys(this.bd.asistencia).some(k => k.endsWith(`|${e.id}`))
      || this.bd.sesiones.some(s => s.registros[e.id]) || this.bd.justificaciones.some(j => j.estudianteId === e.id);
    if (tieneHistorial) {
      e.estadoMatricula = 'Retirada';
      return { mensaje: 'El estudiante tiene asistencia registrada: la matrícula se marcó como retirada para conservar su historial.' };
    }
    this.bd.estudiantes = this.bd.estudiantes.filter(x => x !== e);
    return { mensaje: 'Matrícula anulada.' };
  }

  private estudiante(id: number): Estudiante {
    const e = this.bd.estudiantes.find(x => x.id === id);
    if (!e) throw error(404, `No existe el estudiante con id ${id}.`);
    return e;
  }

  /** El alumno solo accede a sus datos. */
  private estudiantePropio(id: number): Estudiante {
    const s = this.sesion;
    if (s?.perfil === 'alumno' && s.estudianteId !== String(id)) throw error(403, 'Solo puedes consultar tu propia información.');
    return this.estudiante(id);
  }

  private contacto(id: number, c: Cuerpo) {
    const e = this.estudiantePropio(id);
    const correo = limpiar(c['correo'])?.toLowerCase();
    if (correo && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(correo)) throw error(400, 'Ingresa un correo válido.');
    if (limpiar(c['telefono']) && !/^[0-9 +()-]{6,20}$/.test(texto(c['telefono']).trim())) throw error(400, 'Ingresa un teléfono válido.');
    e.correo = correo;
    e.telefono = limpiar(c['telefono']);
    return this.estudianteApi(e);
  }

  // =====================================================================
  // Asistencia diaria
  // =====================================================================

  private validarFecha(fecha: string): void {
    if (fecha > hoy()) throw error(400, 'No se puede registrar asistencia de una fecha futura.');
    if (finDeSemana(fecha)) throw error(400, 'No se registra asistencia en sábado ni domingo.');
  }

  private validarEstado(estado: string): void {
    if (!['presente', 'tardanza', 'falta', 'justificado', 'pendiente'].includes(estado)) {
      throw error(400, `Valor no válido para estado de asistencia: '${estado}'. Valores permitidos: presente, tardanza, falta, justificado, pendiente.`);
    }
  }

  private fijar(estudianteId: number, fecha: string, estado: string): void {
    const k = `${fecha}|${estudianteId}`;
    if (estado === 'pendiente') delete this.bd.asistencia[k];
    else this.bd.asistencia[k] = estado as Estado;
  }

  private rango(p: Parametros) {
    const desde = p['desde'], hasta = p['hasta'];
    if (!desde) throw error(400, "Falta el parámetro 'desde'.");
    if (!hasta) throw error(400, "Falta el parámetro 'hasta'.");
    const ids = new Set(this.activos().filter(e => !p['seccionId'] || e.seccionId === +p['seccionId']).map(e => e.id));
    return Object.entries(this.bd.asistencia)
      .map(([k, estado]) => { const [fecha, id] = k.split('|'); return { estudianteId: +id, fecha, estado }; })
      .filter(r => r.fecha >= desde && r.fecha <= hasta && ids.has(r.estudianteId));
  }

  private registrar(estudianteId: number, fecha: string, estado: string) {
    const e = this.estudiante(estudianteId);
    if (e.estadoMatricula !== 'Activa') throw error(409, 'La matrícula del estudiante está retirada.');
    this.validarEstado(estado);
    this.validarFecha(fecha);
    this.fijar(estudianteId, fecha, estado);
    return { fecha, estado };
  }

  private registrarLote(seccionId: number, c: Cuerpo) {
    const fecha = texto(c['fecha']);
    const registros = (c['registros'] as { estudianteId: number; estado: string }[] | undefined) ?? [];
    if (!fecha || !registros.length) throw error(400, 'Revisa los datos enviados.', ['Indica la fecha de la asistencia.', 'Envía al menos un registro de asistencia.']);
    this.validarFecha(fecha);
    const delAula = new Set(this.activos().filter(e => e.seccionId === seccionId).map(e => e.id));
    for (const r of registros) {
      this.validarEstado(r.estado);
      if (!delAula.has(r.estudianteId)) throw error(400, `El estudiante con id ${r.estudianteId} no está matriculado en esta sección.`);
    }
    registros.forEach(r => this.fijar(r.estudianteId, fecha, r.estado));
    return { fecha };
  }

  private historialDiario(id: number, p: Parametros) {
    this.estudiantePropio(id);
    const hasta = p['hasta'] ?? hoy();
    const desde = p['desde'] ?? '0000-00-00';
    return Object.entries(this.bd.asistencia)
      .filter(([k]) => k.endsWith(`|${id}`))
      .map(([k, estado]) => ({ fecha: k.split('|')[0], estado }))
      .filter(r => r.fecha >= desde && r.fecha <= hasta)
      .sort((a, b) => b.fecha.localeCompare(a.fecha));
  }

  // =====================================================================
  // Justificaciones
  // =====================================================================

  private justificacionApi(j: Justificacion) {
    const e = this.estudiante(j.estudianteId);
    const a = this.bd.asignaciones.find(x => x.id === j.asignacionId);
    return {
      id: j.id, estudianteId: e.id, estudianteNombre: `${e.nombres} ${e.apellidos}`, estudianteDni: e.dni, fecha: j.fecha,
      cursoId: a?.id, cursoNombre: a?.curso, motivo: j.motivo, detalle: j.detalle, archivoNombre: j.archivoNombre, estado: j.estado,
      estadoPrevio: j.estadoPrevio, observacion: j.observacion, fechaSolicitud: j.fechaSolicitud, fechaRevision: j.fechaRevision,
      revisadoPor: j.revisadoPor
    };
  }

  private listarJustificaciones(p: Parametros) {
    const s = this.sesion;
    return this.bd.justificaciones
      .filter(j => s?.perfil !== 'alumno' || String(j.estudianteId) === s.estudianteId)
      .filter(j => !p['docenteId'] || this.bd.asignaciones.find(a => a.id === j.asignacionId)?.docenteId === +p['docenteId'])
      .filter(j => !p['estado'] || j.estado === p['estado'])
      .sort((a, b) => b.fechaSolicitud.localeCompare(a.fechaSolicitud) || b.id - a.id)
      .map(j => this.justificacionApi(j));
  }

  private solicitarJustificacion(c: Cuerpo) {
    const s = this.sesion;
    const estudianteId = s?.perfil === 'alumno' ? Number(s.estudianteId) : Number(c['estudianteId']);
    if (!estudianteId) throw error(400, 'Selecciona el estudiante.');
    const e = this.estudiante(estudianteId);
    const fecha = texto(c['fecha']);
    const motivo = limpiar(c['motivo']);
    if (!fecha) throw error(400, 'Indica la fecha de la inasistencia.');
    if (!motivo) throw error(400, 'Describe el motivo de la inasistencia.');
    if (fecha > hoy()) throw error(400, 'No se puede justificar una fecha futura.');
    if (finDeSemana(fecha)) throw error(400, 'La fecha a justificar debe ser un día de clases (lunes a viernes).');
    const asignacionId = c['cursoId'] ? Number(c['cursoId']) : undefined;
    if (asignacionId) {
      const a = this.asignacion(asignacionId);
      if (a.seccionId !== e.seccionId) throw error(400, 'El curso indicado no corresponde a la sección del estudiante.');
    }
    const mismas = this.bd.justificaciones.filter(j => j.estudianteId === e.id && j.fecha === fecha && j.asignacionId === asignacionId);
    if (mismas.some(j => j.estado === 'Pendiente')) throw error(409, 'Ya existe una solicitud pendiente para esa fecha.');
    if (mismas.some(j => j.estado === 'Aprobada')) throw error(409, 'Esa inasistencia ya fue justificada.');
    const j: Justificacion = {
      id: this.nuevoId(), estudianteId: e.id, asignacionId, fecha, motivo, detalle: limpiar(c['detalle']), archivoNombre: limpiar(c['archivoNombre']),
      estado: 'Pendiente', estadoPrevio: this.bd.asistencia[`${fecha}|${e.id}`] ?? 'pendiente', fechaSolicitud: ahora()
    };
    this.bd.justificaciones.push(j);
    return this.justificacionApi(j);
  }

  /** Aprobar justifica el día (salvo presente) y el curso; observar restaura el estado previo o deja falta. */
  private revisar(id: number, c: Cuerpo) {
    const j = this.bd.justificaciones.find(x => x.id === id);
    if (!j) throw error(404, `No existe la justificación con id ${id}.`);
    if (j.estado !== 'Pendiente') throw error(409, `La justificación ya fue ${j.estado.toLowerCase()}; no se puede revisar.`);
    const estado = texto(c['estado']);
    if (estado !== 'Aprobada' && estado !== 'Observada') throw error(400, 'La revisión debe aprobar u observar la solicitud.');
    const s = this.sesion;
    const a = this.bd.asignaciones.find(x => x.id === j.asignacionId);
    if (s?.perfil === 'docente' && a && String(a.docenteId) !== s.docenteId) throw error(403, 'Solo el docente del curso puede revisar esta justificación.');
    j.estado = estado;
    j.observacion = limpiar(c['observacion']);
    j.fechaRevision = ahora();
    j.revisadoPor = this.bd.usuarios.find(u => String(u.id) === s?.usuarioId)?.nombre;
    if (!finDeSemana(j.fecha)) {
      if (estado === 'Aprobada') {
        if (j.estadoPrevio !== 'presente') this.fijar(j.estudianteId, j.fecha, 'justificado');
      } else {
        const previo = j.estadoPrevio;
        this.fijar(j.estudianteId, j.fecha, previo !== 'justificado' && previo !== 'pendiente' ? previo : 'falta');
      }
    }
    const registro = a ? this.bd.sesiones.find(x => x.asignacionId === a.id && x.fecha === j.fecha)?.registros[j.estudianteId] : undefined;
    if (registro) {
      if (estado === 'Aprobada' && (registro.estado === 'falta' || registro.estado === 'tardanza')) registro.estado = 'justificado';
      if (estado === 'Observada' && registro.estado === 'justificado') registro.estado = 'falta';
    }
    return this.justificacionApi(j);
  }

  // =====================================================================
  // Alertas
  // =====================================================================

  private faltasEn(desde: string, hasta: string): Map<number, number> {
    const faltas = new Map<number, number>();
    for (const [k, estado] of Object.entries(this.bd.asistencia)) {
      const [fecha, id] = k.split('|');
      if (estado === 'falta' && fecha >= desde && fecha <= hasta) faltas.set(+id, (faltas.get(+id) ?? 0) + 1);
    }
    return faltas;
  }

  private alertas() {
    const dia = hoy();
    const ventana = diasHabiles(VENTANA, dia);
    const faltas = this.faltasEn(ventana[0], dia);
    const lista = this.activos()
      .map(e => {
        const total = faltas.get(e.id) ?? 0;
        const faltaHoy = this.bd.asistencia[`${dia}|${e.id}`] === 'falta';
        const avisos = this.bd.avisos.filter(a => a.estudianteId === e.id).map(a => a.enviadoEn).sort();
        return {
          estudiante: this.estudianteApi(e), faltas: total, faltaHoy,
          nivel: total >= CRITICAS ? 'critica' : total >= PREVENTIVAS ? 'preventiva' : 'hoy',
          ultimoAviso: avisos[avisos.length - 1]
        };
      })
      .filter(a => a.faltas >= PREVENTIVAS || a.faltaHoy)
      .sort((a, b) => b.faltas - a.faltas || a.estudiante.nombre.localeCompare(b.estudiante.nombre));
    return {
      parametros: { ventanaDias: VENTANA, faltasCriticas: CRITICAS, faltasPreventivas: PREVENTIVAS, desde: ventana[0], hasta: dia },
      criticas: lista.filter(a => a.nivel === 'critica').length, preventivas: lista.filter(a => a.nivel === 'preventiva').length,
      faltasHoy: lista.filter(a => a.faltaHoy).length, alertas: lista
    };
  }

  private avisar(c: Cuerpo) {
    const ids = [...new Set((c['estudianteIds'] as number[] | undefined) ?? [])];
    if (!ids.length) throw error(400, 'Selecciona al menos un estudiante para avisar a su familia.');
    const dia = hoy();
    const faltas = this.faltasEn(diasHabiles(VENTANA, dia)[0], dia);
    return ids.map(id => {
      this.estudiante(id);
      const aviso = { id: this.nuevoId(), estudianteId: id, enviadoEn: ahora(), faltas: faltas.get(id) ?? 0 };
      this.bd.avisos.push(aviso);
      return aviso;
    });
  }

  // =====================================================================
  // Usuarios
  // =====================================================================

  private usuarioApi(u: Usuario) {
    return {
      id: u.id, nombre: u.nombre, correo: u.correo, perfil: u.perfil, estado: u.estado, creado: u.creado,
      docenteId: this.bd.docentes.find(d => d.usuarioId === u.id)?.id, estudianteId: this.bd.estudiantes.find(e => e.usuarioId === u.id)?.id
    };
  }

  private usuario(id: number): Usuario {
    const u = this.bd.usuarios.find(x => x.id === id);
    if (!u) throw error(404, `No existe el usuario con id ${id}.`);
    return u;
  }

  private guardarUsuario(c: Cuerpo, id?: number) {
    const nombre = limpiar(c['nombre']) ?? '';
    const correo = texto(c['correo']).trim().toLowerCase();
    const errores: string[] = [];
    if (nombre.split(' ').length < 2) errores.push('Ingresa nombres y apellidos.');
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(correo)) errores.push('Ingresa un correo válido.');
    if (errores.length) throw error(400, errores[0], errores);
    if (this.bd.usuarios.some(u => u.correo === correo && u.id !== id)) throw error(409, 'Ese correo ya está registrado.');
    const perfil = texto(c['perfil']) as Perfil;
    const estado = (texto(c['estado']) || 'Activo') as Usuario['estado'];
    if (id) {
      const u = this.usuario(id);
      if (u.perfil === 'administrador' && (perfil !== 'administrador' || estado === 'Inactivo')) this.protegerAdmin(u);
      Object.assign(u, { nombre, correo, perfil, estado });
      return this.usuarioApi(u);
    }
    const u: Usuario = { id: this.nuevoId(), nombre, correo, perfil, estado, creado: hoy(), clave: limpiar(c['contrasena']) ?? '123456' };
    this.bd.usuarios.unshift(u);
    return this.usuarioApi(u);
  }

  private protegerAdmin(u: Usuario): void {
    if (String(u.id) === this.sesion?.usuarioId) throw error(409, 'No puedes desactivar, eliminar ni quitar el perfil de administrador a tu propia cuenta.');
    if (this.bd.usuarios.filter(x => x.perfil === 'administrador' && x.estado === 'Activo').length <= 1) throw error(409, 'Debe quedar al menos un administrador activo.');
  }

  private alternarUsuario(id: number) {
    const u = this.usuario(id);
    if (u.estado === 'Activo' && u.perfil === 'administrador') this.protegerAdmin(u);
    u.estado = u.estado === 'Activo' ? 'Inactivo' : 'Activo';
    return this.usuarioApi(u);
  }

  private eliminarUsuario(id: number) {
    const u = this.usuario(id);
    if (u.perfil === 'administrador' && u.estado === 'Activo') this.protegerAdmin(u);
    if (this.bd.comunicados.some(c => c.autorId === id)) {
      throw error(409, 'La cuenta tiene registros asociados (asistencias, revisiones o comunicados). Desactívala en lugar de eliminarla.');
    }
    this.bd.docentes.filter(d => d.usuarioId === id).forEach(d => delete d.usuarioId);
    this.bd.estudiantes.filter(e => e.usuarioId === id).forEach(e => delete e.usuarioId);
    this.bd.usuarios = this.bd.usuarios.filter(x => x !== u);
    return null;
  }

  // =====================================================================
  // Docente: perfil, cursos, alumnos, sesiones
  // =====================================================================

  private docente(id: number): Docente {
    const s = this.sesion;
    if (s?.perfil === 'docente' && s.docenteId !== String(id)) throw error(403, 'Solo puedes gestionar tu propia información y tus cursos.');
    const d = this.bd.docentes.find(x => x.id === id);
    if (!d) throw error(404, `No existe el docente con id ${id}.`);
    return d;
  }

  private docenteApi(d: Docente) {
    return { ...d, nombreCompleto: `${d.nombres} ${d.apellidos}`, email: this.bd.usuarios.find(u => u.id === d.usuarioId)?.correo };
  }

  private perfilDocente(id: number, c: Cuerpo) {
    const d = this.docente(id);
    const nombres = limpiar(c['nombres']), apellidos = limpiar(c['apellidos']);
    if (!nombres || !apellidos) throw error(400, 'Ingresa tus nombres y apellidos.');
    if (limpiar(c['telefono']) && !/^[0-9 +()-]{6,20}$/.test(texto(c['telefono']).trim())) throw error(400, 'Ingresa un teléfono válido.');
    Object.assign(d, { nombres, apellidos, telefono: limpiar(c['telefono']) });
    const u = this.bd.usuarios.find(x => x.id === d.usuarioId);
    if (u) u.nombre = `${nombres} ${apellidos}`;
    return this.docenteApi(d);
  }

  private asignacion(id: number): Asignacion {
    const a = this.bd.asignaciones.find(x => x.id === id);
    if (!a) throw error(404, `No existe el curso con id ${id}.`);
    return a;
  }

  private asignacionPropia(id: number): Asignacion {
    const a = this.asignacion(id);
    const s = this.sesion;
    if (s?.perfil === 'docente' && String(a.docenteId) !== s.docenteId) throw error(403, 'Este curso no está asignado a tu cuenta.');
    return a;
  }

  private cursoApi(a: Asignacion) {
    const s = this.seccion(a.seccionId);
    const d = this.bd.docentes.find(x => x.id === a.docenteId);
    return {
      id: a.id, codigo: a.codigo, nombre: a.curso, nivel: s.nivel, grado: s.grado, gradoEtiqueta: s.etiqueta, seccion: s.letra, seccionId: s.id,
      docenteId: a.docenteId, docente: d ? `Prof. ${d.nombres} ${d.apellidos}` : '', aula: a.aula, horario: horarioEnTexto(a.horarios),
      horarios: ordenarHorarios(a.horarios), totalAlumnos: this.activos().filter(e => e.seccionId === s.id).length
    };
  }

  private estadosCurso(asignacionId: number, estudianteId: number): EstadoCurso[] {
    return this.bd.sesiones.filter(s => s.asignacionId === asignacionId && s.registros[estudianteId])
      .sort((a, b) => a.fecha.localeCompare(b.fecha)).map(s => s.registros[estudianteId].estado);
  }

  private estudiantesDocente(docenteId: number) {
    this.docente(docenteId);
    return this.bd.asignaciones.filter(a => a.docenteId === docenteId).flatMap(a => {
      const s = this.seccion(a.seccionId);
      return this.activos().filter(e => e.seccionId === a.seccionId).map(e => {
        const estados = this.estadosCurso(a.id, e.id);
        const porcentaje = estados.length ? Math.round(estados.filter(x => x !== 'falta').length * 100 / estados.length) : 100;
        return {
          id: e.id, dni: e.dni, nombres: e.nombres, apellidos: e.apellidos, genero: e.genero, nivel: s.nivel, gradoSeccion: `${s.etiqueta} ${s.letra}`,
          cursoId: a.id, curso: a.curso, cursoNombre: `${a.curso} (${s.etiqueta} ${s.letra})`, sesiones: estados.length, porcentajeAsistencia: porcentaje,
          estadoGeneral: porcentaje >= 80 ? 'Normal' : porcentaje >= 68 ? 'En Riesgo' : 'Crítico'
        };
      });
    });
  }

  private historialDocente(docenteId: number) {
    this.docente(docenteId);
    const propias = new Set(this.bd.asignaciones.filter(a => a.docenteId === docenteId).map(a => a.id));
    return this.bd.sesiones.filter(s => propias.has(s.asignacionId)).sort((a, b) => b.fecha.localeCompare(a.fecha)).map(s => {
      const a = this.asignacion(s.asignacionId);
      const sec = this.seccion(a.seccionId);
      const estados = Object.values(s.registros).map(r => r.estado);
      const contar = (e: EstadoCurso) => estados.filter(x => x === e).length;
      return {
        id: s.id, fecha: s.fecha, cursoId: a.id, cursoNombre: a.curso, gradoSeccion: `${sec.etiqueta} ${sec.letra}`,
        asistieron: contar('asistio'), tardanzas: contar('tardanza'), faltas: contar('falta'), justificados: contar('justificado'), totalAlumnos: estados.length
      };
    });
  }

  private sesionCurso(asignacionId: number, fecha: string) {
    const a = this.asignacionPropia(asignacionId);
    const sesion = this.bd.sesiones.find(s => s.asignacionId === a.id && s.fecha === fecha);
    return {
      yaRegistrado: Boolean(sesion),
      filas: this.activos().filter(e => e.seccionId === a.seccionId).map(e => ({
        alumnoId: e.id, nombres: e.nombres, apellidos: e.apellidos, dni: e.dni,
        estado: sesion?.registros[e.id]?.estado ?? 'asistio', observacion: sesion?.registros[e.id]?.observacion ?? ''
      }))
    };
  }

  private guardarSesion(asignacionId: number, fecha: string, c: Cuerpo) {
    const a = this.asignacionPropia(asignacionId);
    this.validarFecha(fecha);
    const registros = (c['registros'] as { estudianteId: number; estado: EstadoCurso; observacion?: string }[] | undefined) ?? [];
    if (!registros.length) throw error(400, 'Envía la asistencia de al menos un alumno.');
    const delCurso = new Set(this.activos().filter(e => e.seccionId === a.seccionId).map(e => e.id));
    for (const r of registros) {
      if (!delCurso.has(r.estudianteId)) throw error(400, `El alumno con id ${r.estudianteId} no está matriculado en la sección de este curso.`);
      if (!['asistio', 'tardanza', 'falta', 'justificado'].includes(r.estado)) throw error(400, `Valor no válido para estado de asistencia del curso: '${r.estado}'.`);
    }
    let sesion = this.bd.sesiones.find(s => s.asignacionId === a.id && s.fecha === fecha);
    if (!sesion) {
      sesion = { id: this.nuevoId(), asignacionId: a.id, fecha, registros: {} };
      this.bd.sesiones.push(sesion);
    }
    sesion.registros = Object.fromEntries(registros.map(r => [r.estudianteId, { estado: r.estado, observacion: limpiar(r.observacion) ?? '' }]));
    return this.sesionCurso(asignacionId, fecha);
  }

  private resumenMensual(asignacionId: number, mes: string) {
    const a = this.asignacionPropia(asignacionId);
    const sesiones = this.bd.sesiones.filter(s => s.asignacionId === a.id && s.fecha.startsWith(mes));
    return this.activos().filter(e => e.seccionId === a.seccionId).map(e => {
      const estados = sesiones.map(s => s.registros[e.id]?.estado).filter(Boolean);
      const faltas = estados.filter(x => x === 'falta').length;
      return {
        alumnoId: e.id, nombres: e.nombres, apellidos: e.apellidos, dni: e.dni, sesiones: estados.length, asistencias: estados.length - faltas,
        tardanzas: estados.filter(x => x === 'tardanza').length, faltas, porcentaje: estados.length ? Math.round((estados.length - faltas) * 100 / estados.length) : 0
      };
    });
  }

  // =====================================================================
  // Comunicados
  // =====================================================================

  private comunicadoApi(c: Comunicado) {
    const a = this.bd.asignaciones.find(x => x.id === c.asignacionId);
    const s = a ? this.seccion(a.seccionId) : undefined;
    return {
      id: c.id, titulo: c.titulo, contenido: c.contenido, cursoId: a?.id, prioridad: c.prioridad, fechaCreacion: c.fechaCreacion, autorId: c.autorId,
      cursoNombre: a && s ? `${a.curso} - ${s.etiqueta} ${s.letra}` : 'Todos los cursos',
      autor: this.bd.usuarios.find(u => u.id === c.autorId)?.nombre
    };
  }

  private ordenarComunicados(lista: Comunicado[]): Comunicado[] {
    return [...lista].sort((a, b) => b.fechaCreacion.localeCompare(a.fechaCreacion) || b.id - a.id);
  }

  private listarComunicados() {
    const s = this.sesion;
    let lista = this.bd.comunicados;
    if (s?.perfil === 'alumno') {
      const e = this.estudiante(Number(s.estudianteId));
      lista = lista.filter(c => !c.asignacionId || this.bd.asignaciones.find(a => a.id === c.asignacionId)?.seccionId === e.seccionId);
    } else if (s?.perfil === 'docente') {
      lista = lista.filter(c => String(c.autorId) === s.usuarioId || String(this.bd.asignaciones.find(a => a.id === c.asignacionId)?.docenteId) === s.docenteId);
    }
    return this.ordenarComunicados(lista).map(c => this.comunicadoApi(c));
  }

  private publicar(c: Cuerpo) {
    const titulo = limpiar(c['titulo']), contenido = texto(c['contenido']).trim();
    if (!titulo || !contenido) throw error(400, 'Ingresa el título y el contenido del comunicado.');
    if (!['Alta', 'Media', 'Baja'].includes(texto(c['prioridad']))) throw error(400, `Valor no válido para prioridad: '${texto(c['prioridad'])}'. Valores permitidos: Alta, Media, Baja.`);
    const asignacionId = c['cursoId'] ? Number(c['cursoId']) : undefined;
    if (asignacionId) {
      const a = this.asignacion(asignacionId);
      const s = this.sesion;
      if (s?.perfil === 'docente' && String(a.docenteId) !== s.docenteId) throw error(403, 'Solo puedes publicar comunicados en tus cursos.');
    }
    const comunicado: Comunicado = {
      id: this.nuevoId(), titulo, contenido, prioridad: texto(c['prioridad']) as Comunicado['prioridad'], asignacionId,
      autorId: Number(this.sesion?.usuarioId) || undefined, fechaCreacion: hoy()
    };
    this.bd.comunicados.push(comunicado);
    return this.comunicadoApi(comunicado);
  }

  private eliminarComunicado(id: number) {
    const c = this.bd.comunicados.find(x => x.id === id);
    if (!c) throw error(404, `No existe el comunicado con id ${id}.`);
    const s = this.sesion;
    if (s?.perfil !== 'administrador' && String(c.autorId) !== s?.usuarioId) throw error(403, 'Solo el autor o un administrador puede modificar este comunicado.');
    this.bd.comunicados = this.bd.comunicados.filter(x => x !== c);
    return null;
  }

  // =====================================================================
  // Portal del alumno
  // =====================================================================

  private cursosAlumno(id: number) {
    const e = this.estudiantePropio(id);
    return this.bd.asignaciones.filter(a => a.seccionId === e.seccionId).map(a => this.cursoApi(a));
  }

  private asistenciaAlumno(id: number, p: Parametros) {
    const e = this.estudiantePropio(id);
    const dia = hoy();
    const hasta = p['hasta'] && p['hasta'] < dia ? p['hasta'] : dia;
    const inicio = new Date(`${hasta}T12:00:00`);
    inicio.setDate(inicio.getDate() - 120);
    const desde = p['desde'] ?? iso(inicio);
    const cursoId = p['cursoId'] ? Number(p['cursoId']) : undefined;
    const observadas = this.bd.justificaciones.filter(j => j.estudianteId === e.id && j.estado === 'Observada');
    const observacion = (fecha: string, asignacionId?: number) =>
      observadas.find(j => j.fecha === fecha && (!j.asignacionId || j.asignacionId === asignacionId))?.observacion;
    const diario: Record<EstadoCurso, Estado> = { asistio: 'presente', tardanza: 'tardanza', falta: 'falta', justificado: 'justificado' };

    const dias: { fecha: string; estado: string; cursoId?: number; cursoNombre?: string; docente?: string; hora?: string; observacion?: string }[] = [];
    const conCurso = new Set<string>();
    for (const s of this.bd.sesiones) {
      const r = s.registros[e.id];
      if (!r || s.fecha < desde || s.fecha > hasta || (cursoId && s.asignacionId !== cursoId)) continue;
      const a = this.asignacion(s.asignacionId);
      const d = this.bd.docentes.find(x => x.id === a.docenteId);
      const bloque = ordenarHorarios(a.horarios).find(h => h.dia === diaSemana(s.fecha));
      conCurso.add(s.fecha);
      dias.push({
        fecha: s.fecha, estado: diario[r.estado], cursoId: a.id, cursoNombre: a.curso, docente: d ? `Prof. ${d.nombres} ${d.apellidos}` : undefined,
        hora: bloque ? `${bloque.inicio}–${bloque.fin}` : undefined, observacion: r.observacion || observacion(s.fecha, a.id)
      });
    }
    if (!cursoId) {
      for (const r of this.historialDiario(e.id, { desde, hasta })) {
        if (!conCurso.has(r.fecha)) dias.push({ fecha: r.fecha, estado: r.estado, observacion: observacion(r.fecha) });
      }
      if (!dias.some(d => d.fecha === dia) && !finDeSemana(dia) && hasta >= dia) dias.push({ fecha: dia, estado: 'pendiente' });
    }
    dias.sort((a, b) => b.fecha.localeCompare(a.fecha) || (a.cursoNombre ?? '').localeCompare(b.cursoNombre ?? ''));
    const limite = p['limite'] ? Number(p['limite']) : p['desde'] ? Number.MAX_SAFE_INTEGER : 20;
    return dias.slice(0, limite);
  }

  private alertasAlumno(id: number) {
    const e = this.estudiantePropio(id);
    const dia = hoy();
    const lista: { id: string; fecha: string; titulo: string; detalle: string; tipo: string; leido: boolean }[] = [];
    const estadoHoy = this.bd.asistencia[`${dia}|${e.id}`];
    if (estadoHoy === 'falta') lista.push({ id: 'falta-hoy', fecha: dia, titulo: 'Falta registrada', detalle: 'Revisa el registro de hoy y comunícate con el colegio si necesitas aclararlo.', tipo: 'asistencia', leido: false });
    if (estadoHoy === 'tardanza') lista.push({ id: 'tardanza-hoy', fecha: dia, titulo: 'Tardanza registrada', detalle: 'Se registró una tardanza en tu asistencia de hoy.', tipo: 'asistencia', leido: false });
    const contados = this.asistenciaAlumno(id, {}).filter(d => d.estado !== 'pendiente');
    const porcentaje = contados.length ? Math.round(contados.filter(d => d.estado !== 'falta').length * 100 / contados.length) : 100;
    if (contados.length >= 5 && porcentaje < 85) {
      lista.push({ id: 'porcentaje-bajo', fecha: dia, titulo: 'Porcentaje de asistencia bajo', detalle: `Tu asistencia está en ${porcentaje}%. Revisa tu historial y conversa con el colegio.`, tipo: 'asistencia', leido: false });
    }
    this.bd.avisos.filter(a => a.estudianteId === e.id).slice(-5).forEach(a => lista.push({
      id: `aviso-${a.id}`, fecha: a.enviadoEn.slice(0, 10), titulo: 'Aviso enviado a tu familia',
      detalle: `El colegio comunicó a tu familia ${a.faltas} falta(s) acumulada(s).`, tipo: 'asistencia', leido: false
    }));
    this.bd.justificaciones.filter(j => j.estudianteId === e.id && j.estado !== 'Pendiente').slice(-10).forEach(j => lista.push({
      id: `justificacion-${j.id}`, fecha: j.fecha, titulo: `Justificación ${j.estado.toLowerCase()}`,
      detalle: j.observacion ?? `${j.motivo}${j.archivoNombre ? ` · Sustento: ${j.archivoNombre}` : ''}`, tipo: 'justificacion', leido: false
    }));
    return lista.sort((a, b) => b.fecha.localeCompare(a.fecha));
  }

  private comunicacionesAlumno(id: number) {
    const e = this.estudiantePropio(id);
    const visibles = this.bd.comunicados.filter(c => !c.asignacionId || this.bd.asignaciones.find(a => a.id === c.asignacionId)?.seccionId === e.seccionId);
    return this.ordenarComunicados(visibles).map(c => {
      const a = this.bd.asignaciones.find(x => x.id === c.asignacionId);
      return {
        id: `comunicado-${c.id}`, fecha: c.fechaCreacion, titulo: c.prioridad === 'Alta' ? `Importante: ${c.titulo}` : c.titulo,
        detalle: `${c.contenido} · ${a ? a.curso : 'Todos los cursos'}`, tipo: 'comunicado', leido: false
      };
    });
  }

  // =====================================================================
  // Almacenamiento y datos de demostración
  // =====================================================================

  private seccion(id: number): Seccion {
    const s = this.bd.secciones.find(x => x.id === id);
    if (!s) throw error(404, `No existe la sección con id ${id}.`);
    return s;
  }

  private nuevoId(): number {
    return this.bd.siguienteId++;
  }

  private cargar(): Bd {
    try {
      const guardada = JSON.parse(localStorage.getItem(BD_KEY) ?? 'null') as Bd | null;
      if (guardada?.version === VERSION) return guardada;
    } catch { /* datos dañados: se vuelven a generar */ }
    const nueva = this.sembrar();
    this.bd = nueva;
    this.guardar();
    return nueva;
  }

  private guardar(): void {
    try {
      localStorage.setItem(BD_KEY, JSON.stringify(this.bd));
    } catch {
      console.warn('No se pudo guardar el modo local en este navegador (almacenamiento lleno).');
    }
  }

  /** Mismos datos que carga el backend (DatosIniciales.java): estructura, cuentas, docentes, 12 alumnos por aula, cursos e historial. */
  private sembrar(): Bd {
    let siguiente = 1;
    const nuevoId = () => siguiente++;
    const anio = hoy().slice(0, 4);
    const niveles = [
      { nombre: 'Inicial', codigos: ['3 años', '4 años', '5 años'] },
      { nombre: 'Primaria', codigos: ['1', '2', '3', '4', '5', '6'] },
      { nombre: 'Secundaria', codigos: ['1', '2', '3', '4', '5'] }
    ];
    const secciones: Seccion[] = [];
    for (const n of niveles) {
      for (const codigo of n.codigos) {
        for (const letra of ['A', 'B', 'C']) {
          secciones.push({ id: nuevoId(), nivel: n.nombre, grado: codigo, etiqueta: etiquetaGrado(n.nombre, codigo), letra, turno: letra === 'C' ? 'Tarde' : 'Mañana' });
        }
      }
    }
    const seccionDe = (nivel: string, grado: string, letra: string) => secciones.find(s => s.nivel === nivel && s.grado === grado && s.letra === letra)!;

    const usuario = (nombre: string, correo: string, perfil: Perfil, estado: Usuario['estado'] = 'Activo'): Usuario =>
      ({ id: nuevoId(), nombre, correo: `${correo}@ie20191.edu.pe`, perfil, estado, creado: `${anio}-03-01`, clave: '123456' });
    const admin = usuario('Administrador General', 'admin', 'administrador');
    const cuentas = {
      elena: usuario('Elena Salazar Ríos', 'elena.salazar', 'docente'), luis: usuario('Luis Mendoza Castro', 'luis.mendoza', 'docente'),
      carmen: usuario('Carmen Rojas Villa', 'carmen.rojas', 'docente'), jorge: usuario('Jorge Paredes Lima', 'jorge.paredes', 'docente'),
      rosa: usuario('Rosa Huamán Quispe', 'rosa.huaman', 'docente'), carlos: usuario('Carlos Antonio Mendoza Ríos', 'carlos.mendoza', 'docente'),
      lucia: usuario('Lucía Bustamante Morales', 'lucia.bustamante', 'alumno'), juan: usuario('Juan Alvarado Mendoza', 'juan.alvarado', 'alumno', 'Inactivo')
    };

    const docente = (nombres: string, apellidos: string, dni: string, cuenta: Usuario | undefined, especialidad: string, extra: Partial<Docente> = {}): Docente =>
      ({ id: nuevoId(), usuarioId: cuenta?.id, nombres, apellidos, dni, especialidad, ...extra });
    const d = {
      carlos: docente('Carlos Antonio', 'Mendoza Ríos', '41829304', cuentas.carlos, 'Matemática y Ciencias',
        { telefono: '987 654 321', colegiatura: 'CPPe N° 45812', codigoPlaza: 'PLZ-2026-9921', condicion: 'Nombrado', nivelEducativo: 'Secundaria', fechaIngreso: '2018-03-15' }),
      elena: docente('Elena', 'Salazar Ríos', '41829305', cuentas.elena, 'Comunicación'),
      luis: docente('Luis', 'Mendoza Castro', '41829306', cuentas.luis, 'Matemática'),
      carmen: docente('Carmen', 'Rojas Villa', '41829307', cuentas.carmen, 'Personal Social'),
      jorge: docente('Jorge', 'Paredes Lima', '41829308', cuentas.jorge, 'Ciencia y Tecnología'),
      rosa: docente('Rosa', 'Huamán Quispe', '41829309', cuentas.rosa, 'Educación Física'),
      diana: docente('Diana', 'Quispe', '41829310', undefined, 'Arte y Cultura'),
      mario: docente('Mario', 'Torres', '41829311', undefined, 'Inglés'),
      ana: docente('Ana', 'Castillo', '41829312', undefined, 'Educación Religiosa')
    };

    const nombres = ['Lucía Bustamante Morales', 'Juan Alvarado Mendoza', 'Sofía Castro Villanueva', 'Carlos Díaz Peña', 'Valeria Flores Rojas',
      'Mateo García Pérez', 'Camila Huamán Salas', 'Diego López Cárdenas', 'Andrea Medina Torres', 'Thiago Navarro Ruiz', 'Daniela Ortega León', 'Sebastián Ramos Vega'];
    const femeninos = new Set(['Lucía', 'Sofía', 'Valeria', 'Camila', 'Andrea', 'Daniela']);
    const estudiantes: Estudiante[] = [];
    const estudiante = (nombre: string, dni: string, seccion: Seccion): Estudiante => {
      const [n, a] = separarNombre(nombre)!;
      const e: Estudiante = { id: nuevoId(), matriculaId: nuevoId(), nombres: n, apellidos: a, dni, genero: femeninos.has(n) ? 'F' : 'M', seccionId: seccion.id, estadoMatricula: 'Activa' };
      estudiantes.push(e);
      return e;
    };
    let numero = 0;
    for (const s of secciones) for (const nombre of nombres) estudiante(nombre, String(70_000_000 + ++numero).padStart(8, '0'), s);
    const primeroA = seccionDe('Primaria', '1', 'A');
    const juanPerez = estudiante('Juan Pérez Ugarte', '72819201', primeroA);
    for (const e of estudiantes.filter(x => x.seccionId === primeroA.id)) {
      if (`${e.nombres} ${e.apellidos}` === 'Lucía Bustamante Morales') Object.assign(e, { usuarioId: cuentas.lucia.id, correo: cuentas.lucia.correo });
      if (`${e.nombres} ${e.apellidos}` === 'Juan Alvarado Mendoza') e.usuarioId = cuentas.juan.id;
    }

    const asignacion = (codigo: string, curso: string, seccion: Seccion, doc: Docente, aula: string, ...bloques: string[]): Asignacion => ({
      id: nuevoId(), codigo, curso, seccionId: seccion.id, docenteId: doc.id, aula,
      horarios: bloques.map(b => { const [dia, rango] = b.split(' '); const [inicio, fin] = rango.split('-'); return { dia, inicio, fin }; })
    });
    const asignaciones = [
      asignacion('COM-1A', 'Comunicación', primeroA, d.elena, 'Aula 04', 'Lunes 08:00-09:15', 'Miércoles 09:15-10:30', 'Jueves 11:00-12:30'),
      asignacion('MAT-1A', 'Matemática', primeroA, d.luis, 'Aula 04', 'Lunes 09:15-10:30', 'Martes 11:00-12:30', 'Jueves 08:00-09:15', 'Viernes 11:00-12:30'),
      asignacion('PS-1A', 'Personal Social', primeroA, d.carmen, 'Aula 04', 'Martes 08:00-09:15', 'Viernes 09:15-10:30'),
      asignacion('CYT-1A', 'Ciencia y Tecnología', primeroA, d.jorge, 'Aula 04', 'Martes 09:15-10:30', 'Miércoles 11:00-12:30', 'Jueves 09:15-10:30'),
      asignacion('EF-1A', 'Educación Física', primeroA, d.rosa, 'Patio principal', 'Miércoles 08:00-09:15', 'Viernes 08:00-09:15'),
      asignacion('AYC-1A', 'Arte y Cultura', primeroA, d.diana, 'Aula 04', 'Lunes 11:00-12:30', 'Jueves 12:30-14:00'),
      asignacion('ING-1A', 'Inglés', primeroA, d.mario, 'Aula 04', 'Martes 12:30-14:00', 'Viernes 12:30-14:00'),
      asignacion('REL-1A', 'Educación Religiosa', primeroA, d.ana, 'Aula 04', 'Lunes 12:30-14:00'),
      asignacion('TUT-1A', 'Tutoría', primeroA, d.elena, 'Aula 04', 'Miércoles 12:30-14:00'),
      asignacion('PSI-5A', 'Psicomotriz', seccionDe('Inicial', '5 años', 'A'), d.diana, 'Patio de Juegos / Aula 01', 'Lunes 08:30-09:15', 'Miércoles 08:30-09:15'),
      asignacion('COM-4B', 'Comunicación integral', seccionDe('Inicial', '4 años', 'B'), d.elena, 'Aula Inicial 02', 'Martes 09:30-10:15', 'Jueves 09:30-10:15'),
      asignacion('MAT-3A', 'Matemática', seccionDe('Primaria', '3', 'A'), d.carmen, 'Aula 201', 'Lunes 08:00-09:30', 'Miércoles 08:00-09:30'),
      asignacion('GEO-2B', 'Personal Social', seccionDe('Primaria', '2', 'B'), d.ana, 'Aula 104', 'Martes 08:00-09:30', 'Jueves 08:00-09:30'),
      asignacion('EDF-4A', 'Educación Física', seccionDe('Primaria', '4', 'A'), d.rosa, 'Losa Deportiva', 'Miércoles 10:00-11:30'),
      asignacion('MAT-3B', 'Matemática', seccionDe('Secundaria', '3', 'B'), d.carlos, 'Aula 202', 'Martes 10:00-11:30', 'Jueves 10:00-11:30'),
      asignacion('FIS-4A', 'Física Elemental (Ciencia y Tecnología)', seccionDe('Secundaria', '4', 'A'), d.jorge, 'Laboratorio 1', 'Viernes 08:00-11:00'),
      asignacion('ALG-5A', 'Álgebra Avanzada', seccionDe('Secundaria', '5', 'A'), d.carlos, 'Aula 301', 'Lunes 09:15-10:30', 'Miércoles 09:15-10:30'),
      asignacion('DPCC-4B', 'Desarrollo Personal, Ciudadanía y Cívica', seccionDe('Secundaria', '4', 'B'), d.carlos, 'Aula 205', 'Lunes 11:30-13:00', 'Miércoles 11:30-13:00'),
      asignacion('EPT-5B', 'Educación para el Trabajo (Computación)', seccionDe('Secundaria', '5', 'B'), d.carlos, 'Aula de Cómputo', 'Jueves 11:30-13:00'),
      asignacion('RM-3C', 'Razonamiento Matemático', seccionDe('Secundaria', '3', 'C'), d.carlos, 'Aula 203', 'Viernes 09:30-11:00')
    ];

    // Historial: 22 días hábiles anteriores a hoy, con el mismo reparto que el backend.
    const dia = hoy();
    const fechas = diasHabiles(23, dia).filter(f => f < dia);
    const asistencia: Record<string, Estado> = {};
    for (const e of estudiantes) {
      const perfil = hash(e.dni) % 12;
      const probabilidad = perfil === 0 ? .2 : perfil === 1 ? .11 : .025;
      for (const f of fechas) {
        const azar = (hash(`${e.dni}|${f}`) % 1000) / 1000;
        asistencia[`${f}|${e.id}`] = (e === juanPerez && f === fechas[fechas.length - 1]) ? 'falta'
          : azar < probabilidad ? 'falta' : azar < probabilidad + .05 ? 'tardanza' : azar < probabilidad + .07 ? 'justificado' : 'presente';
      }
    }
    const enCurso: Record<Estado, EstadoCurso> = { presente: 'asistio', tardanza: 'tardanza', falta: 'falta', justificado: 'justificado' };
    const sesiones: Sesion[] = [];
    const faltasCurso: { e: Estudiante; a: Asignacion; fecha: string }[] = [];
    for (const a of asignaciones) {
      for (const f of fechas) {
        if (!a.horarios.some(h => h.dia === diaSemana(f))) continue;
        const registros: Sesion['registros'] = {};
        for (const e of estudiantes.filter(x => x.seccionId === a.seccionId)) {
          const estado = asistencia[`${f}|${e.id}`];
          registros[e.id] = { estado: enCurso[estado], observacion: '' };
          if (estado === 'falta' && a.seccionId === primeroA.id && e !== juanPerez) faltasCurso.push({ e, a, fecha: f });
        }
        sesiones.push({ id: nuevoId(), asignacionId: a.id, fecha: f, registros });
      }
    }

    const justificaciones: Justificacion[] = [];
    if (fechas.length) {
      const f = fechas[fechas.length - 1];
      justificaciones.push({ id: nuevoId(), estudianteId: juanPerez.id, fecha: f, motivo: 'Cita médica comunicada por la familia.', estado: 'Pendiente', estadoPrevio: 'falta', fechaSolicitud: `${f}T18:00:00` });
    }
    [['Cita Médica Sanitas', 'Adjunto constancia de atención médica emitida por la clínica.'], ['Salud / Gripe fuerte', 'Descanso médico por 48 horas.']]
      .forEach(([motivo, detalle], i) => {
        const falta = faltasCurso[faltasCurso.length - 1 - i];
        if (falta) justificaciones.push({ id: nuevoId(), estudianteId: falta.e.id, asignacionId: falta.a.id, fecha: falta.fecha, motivo, detalle, estado: 'Pendiente', estadoPrevio: 'falta', fechaSolicitud: `${falta.fecha}T18:00:00` });
      });

    const haceDias = (n: number) => { const x = new Date(); x.setDate(x.getDate() - n); return iso(x); };
    const mat3a = asignaciones.find(a => a.codigo === 'MAT-3A')!;
    const comunicados: Comunicado[] = [
      { id: nuevoId(), titulo: 'Evaluación Parcial de Matemática', contenido: 'Se recuerda a los estudiantes que el examen parcial se realizará este viernes.', prioridad: 'Alta', asignacionId: mat3a.id, autorId: cuentas.carmen.id, fechaCreacion: haceDias(3) },
      { id: nuevoId(), titulo: 'Revisa tu asistencia', contenido: 'Consulta periódicamente las asistencias de tus cursos y avisa al colegio si identificas un dato que deba revisarse.', prioridad: 'Media', autorId: admin.id, fechaCreacion: haceDias(7) },
      { id: nuevoId(), titulo: 'Justificaciones', contenido: 'Las solicitudes de inasistencia requieren revisión del personal autorizado antes de actualizar el registro.', prioridad: 'Baja', autorId: admin.id, fechaCreacion: haceDias(7) }
    ];

    return {
      version: VERSION,
      siguienteId: siguiente,
      institucion: { nombre: 'I.E. 20191 Alfonso Ugarte', codigoModular: '0285940', direccion: 'San Vicente de Cañete, Lima, Perú', colorPrincipal: '#9b1b1f', colorSecundario: '#fdc425', logo: '' },
      niveles: niveles.map(n => ({ nombre: n.nombre, grados: n.codigos.map(c => ({ codigo: c, etiqueta: etiquetaGrado(n.nombre, c) })) })),
      secciones,
      fases: [
        { semana: '01', titulo: 'Exploración', actividad: 'Investigación de la comunicación dispersa y el control manual de asistencia.', producto: 'Ficha de diagnóstico institucional', estado: 'Completada' },
        { semana: '02', titulo: 'Planeación', actividad: 'Análisis de hallazgos, componentes e infraestructura para orientar la solución.', producto: 'Plan del proyecto', estado: 'Completada' },
        { semana: '03', titulo: 'Requerimientos', actividad: 'Definición de la matriz transaccional, justificaciones y alertas tempranas.', producto: 'Documento de requerimientos', estado: 'Completada' },
        { semana: '04', titulo: 'Diseño técnico', actividad: 'Diseño de pantallas y modelado de la base de datos relacional.', producto: 'Prototipo y arquitectura técnica', estado: 'En curso' },
        { semana: '05–06', titulo: 'Desarrollo parcial', actividad: 'Construcción web e integración de servicios y conexión a datos.', producto: 'Versión parcial del sistema web', estado: 'Pendiente' },
        { semana: '07', titulo: 'Validación', actividad: 'Pruebas de asistencia, justificaciones y alertas con retroalimentación.', producto: 'Informe de validación', estado: 'Pendiente' },
        { semana: '08', titulo: 'Presentación final', actividad: 'Demostración del sistema y sustentación de la solución.', producto: 'Presentación final', estado: 'Pendiente' }
      ],
      usuarios: [admin, ...Object.values(cuentas)],
      docentes: Object.values(d),
      estudiantes,
      asignaciones,
      asistencia,
      sesiones,
      justificaciones,
      avisos: [],
      comunicados
    };
  }
}
