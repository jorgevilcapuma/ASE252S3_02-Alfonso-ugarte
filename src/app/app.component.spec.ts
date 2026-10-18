import { registerLocaleData } from '@angular/common';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import localeEsPe from '@angular/common/locales/es-PE';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { API_URL } from './core/config/api';
import { authInterceptor } from './core/interceptors/auth.interceptor';
import { AsistenciaService } from './core/services/asistencia.service';
import { AuthService } from './core/services/auth.service';

registerLocaleData(localeEsPe);

/** EstudianteResponse del backend (tabla estudiantes). */
const ESTUDIANTE = {
  id: 7, nombre: 'Juan Pérez Ugarte', nombres: 'Juan', apellidos: 'Pérez Ugarte', dni: '72819201', genero: 'M', gradoSeccionId: 4,
  nivel: 'Primaria', grado: '1', gradoEtiqueta: '1ro grado', seccion: 'A', turno: 'Mañana', estado: 'A'
};

/** Deja correr las promesas pendientes (el adaptador encadena varias peticiones). */
const tick = () => new Promise(resolver => setTimeout(resolver));

/**
 * Pruebas del modo servidor: las pantallas usan las rutas de siempre y el adaptador (core/servidor/api-servidor.ts)
 * las traduce a la API del backend ASE252S3_BE. Las llamadas HTTP se simulan con HttpTestingController.
 */
describe('Portal conectado al backend', () => {
  let http: HttpTestingController;
  let auth: AuthService;
  let data: AsistenciaService;

  async function ingresarComoAdministrador(): Promise<void> {
    const promesa = auth.autenticar({ perfil: 'administrador', correo: 'admin@ie20191.edu.pe', contrasena: '123456' });
    await tick();
    http.expectOne(`${API_URL}/auth/login`).flush({
      token: 'token-de-prueba', expiraEn: Date.now() + 3_600_000,
      sesion: { usuarioId: 1, correo: 'admin@ie20191.edu.pe', nombre: 'Administrador General', perfil: 'administrador', estudianteId: null, docenteId: null, dni: null }
    });
    auth.abrirSesion(await promesa);
  }

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(withInterceptors([authInterceptor])), provideHttpClientTesting()]
    });
    http = TestBed.inject(HttpTestingController);
    auth = TestBed.inject(AuthService);
    data = TestBed.inject(AsistenciaService);
  });

  afterEach(() => http.verify());

  it('abre la sesión con el token del backend y lo envía en cada petición', async () => {
    await ingresarComoAdministrador();
    expect(auth.isAuthenticated).toBeTrue();
    expect(auth.sesion?.usuarioId).toBe('1');
    expect(auth.rutaInicial()).toBe('/administrador/dashboard');

    const carga = data.cargarEstructura();
    await tick();
    const peticion = http.expectOne(r => r.url === `${API_URL}/grados-secciones`);
    expect(peticion.request.headers.get('Authorization')).toBe('Bearer token-de-prueba');
    peticion.flush([]);
    await carga;

    // Las fases del proyecto son fijas: no van al backend.
    await data.cargarFases();
    expect(data.fases[0].titulo).toBe('Exploración');

    auth.signOut();
    expect(auth.isAuthenticated).toBeFalse();
  });

  it('muestra el mensaje del backend cuando el ingreso falla', async () => {
    const promesa = auth.autenticar({ perfil: 'docente', correo: 'x@ie20191.edu.pe', contrasena: 'incorrecta' });
    await tick();
    http.expectOne(`${API_URL}/auth/login`).flush(
      { status: 401, mensaje: 'Correo o contraseña incorrectos.', errores: ['Correo o contraseña incorrectos.'] },
      { status: 401, statusText: 'Unauthorized' });
    await expectAsync(promesa).toBeRejectedWithError('Correo o contraseña incorrectos.');
    expect(auth.isAuthenticated).toBeFalse();
  });

  it('arma la estructura académica desde /grados-secciones y el padrón desde /estudiantes', async () => {
    await ingresarComoAdministrador();
    const estructura = data.cargarEstructura();
    await tick();
    const aulas = http.expectOne(r => r.url === `${API_URL}/grados-secciones`);
    expect(aulas.request.params.get('estado')).toBe('A');
    aulas.flush([
      { id: 1, nivel: 'Inicial', grado: '3 años', gradoEtiqueta: '3 años', seccion: 'A', turno: 'Mañana', totalEstudiantes: 12, estado: 'A' },
      { id: 4, nivel: 'Primaria', grado: '1', gradoEtiqueta: '1ro grado', seccion: 'A', turno: 'Mañana', totalEstudiantes: 13, estado: 'A' },
      { id: 6, nivel: 'Primaria', grado: '1', gradoEtiqueta: '1ro grado', seccion: 'C', turno: 'Tarde', totalEstudiantes: 12, estado: 'A' }
    ]);
    await estructura;
    expect(data.niveles).toEqual(['Inicial', 'Primaria']);
    expect(data.gradosPorNivel.Primaria).toEqual(['1']);
    expect(data.seccionId('Primaria', '1', 'A')).toBe('4');
    expect(data.turnoSeccion('C')).toBe('Tarde');
    expect(data.etiquetaGrado('Primaria', '1')).toBe('1ro grado');

    const padron = data.cargarEstudiantes();
    await tick();
    http.expectOne(r => r.url === `${API_URL}/estudiantes` && r.params.get('estado') === 'A').flush([ESTUDIANTE]);
    await padron;
    expect(data.estudiantesDeAula('Primaria', '1', 'A').map(e => e.id)).toEqual(['7']);
  });

  it('guarda la asistencia en /asistencia-estudiantes y la revierte si el servidor la rechaza', async () => {
    await ingresarComoAdministrador();
    const fecha = data.hoy;

    const guardado = data.guardarEstado('7', fecha, 'falta');
    expect(data.obtenerEstado('7', fecha)).toBe('falta');
    await tick();
    const peticion = http.expectOne(`${API_URL}/asistencia-estudiantes/estudiantes/7`);
    expect(peticion.request.method).toBe('PUT');
    expect(peticion.request.body).toEqual({ fecha, estado: 'falta' });
    peticion.flush({ fecha, cursoId: null, estado: 'falta' });
    expect(await guardado).toBeTrue();

    const rechazado = data.guardarEstado('7', fecha, 'presente');
    await tick();
    http.expectOne(`${API_URL}/asistencia-estudiantes/estudiantes/7`).flush(
      { mensaje: 'No se registra asistencia en sábado ni domingo.' }, { status: 400, statusText: 'Bad Request' });
    expect(await rechazado).toBeFalse();
    expect(data.obtenerEstado('7', fecha)).toBe('falta');
  });

  it('carga la asistencia de un rango con la matriz mensual del aula', async () => {
    await ingresarComoAdministrador();
    const carga = data.cargarAsistencia('2026-10-01', '2026-10-13', '4');
    await tick();
    const peticion = http.expectOne(r => r.url === `${API_URL}/asistencia-estudiantes/grados-secciones/4/mensual`);
    expect(peticion.request.params.get('mes')).toBe('2026-10');
    expect(peticion.request.params.has('cursoId')).toBeFalse();
    peticion.flush({ filas: [{ estudianteId: 7, estados: { '2026-10-12': 'tardanza', '2026-10-13': 'pendiente', '2026-10-20': 'falta' } }] });
    await carga;
    expect(data.obtenerEstado('7', '2026-10-12')).toBe('tardanza');
    expect(data.obtenerEstado('7', '2026-10-13')).toBe('pendiente');
    expect(data.obtenerEstado('7', '2026-10-20')).toBe('pendiente');   // fuera del rango pedido
  });

  it('al revisar una justificación toma del backend el estado resultante del día', async () => {
    await ingresarComoAdministrador();
    const fecha = '2026-10-12';
    const solicitud = data.registrarJustificacion('7', fecha, 'Cita médica');
    await tick();
    const creada = http.expectOne(`${API_URL}/justificaciones`);
    expect(creada.request.body).toEqual(jasmine.objectContaining({ estudianteId: 7, fecha, motivo: 'Cita médica' }));
    creada.flush({ id: 3, estudianteId: 7, estudianteNombre: 'Juan Pérez Ugarte', fecha, motivo: 'Cita médica', estado: 'Pendiente', estadoPrevio: 'falta', fechaSolicitud: '2026-10-12T18:00:00' });
    expect(await solicitud).toBeNull();
    expect(data.justificaciones[0].id).toBe(3);

    const revision = data.revisarJustificacion(data.justificaciones[0], 'Aprobada');
    await tick();
    http.expectOne(`${API_URL}/justificaciones/3/revision`).flush({ ...data.justificaciones[0], estudianteId: 7, estado: 'Aprobada', fechaSolicitud: '2026-10-12T18:00:00' });
    await tick();
    // El historial del backend trae el registro diario y los de cada curso: solo cuenta el diario.
    http.expectOne(r => r.url === `${API_URL}/estudiantes/7/asistencia`).flush([
      { fecha, cursoId: 2, cursoNombre: 'Matemática', estado: 'falta' },
      { fecha, cursoId: null, estado: 'justificado' }
    ]);
    expect(await revision).toBeNull();
    expect(data.justificaciones[0].estado).toBe('Aprobada');
    expect(data.obtenerEstado('7', fecha)).toBe('justificado');
  });

  it('limita las rutas según el perfil de la sesión', async () => {
    await ingresarComoAdministrador();
    expect(auth.puedeAcceder('/administrador/estudiantes')).toBeTrue();
    expect(auth.puedeAcceder('/docente/dashboard')).toBeFalse();
    expect(auth.puedeAcceder('/alumno/inicio')).toBeFalse();
  });
});
