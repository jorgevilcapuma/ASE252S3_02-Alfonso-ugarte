import { registerLocaleData } from '@angular/common';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import localeEsPe from '@angular/common/locales/es-PE';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { API_URL } from './core/config/api';
import { authInterceptor } from './core/interceptors/auth.interceptor';
import { EstudianteApi } from './core/models/asistencia.model';
import { AsistenciaService } from './core/services/asistencia.service';
import { AuthService } from './core/services/auth.service';

registerLocaleData(localeEsPe);

const ESTUDIANTE: EstudianteApi = {
  id: 7, nombre: 'Juan Pérez Ugarte', nombres: 'Juan', apellidos: 'Pérez Ugarte', dni: '72819201', genero: 'M',
  nivel: 'Primaria', grado: '1', gradoEtiqueta: '1ro grado', seccion: 'A', seccionId: 4, turno: 'Mañana',
  matriculaId: 9, estadoMatricula: 'Activa'
};

/** Pruebas del cliente del backend: las llamadas HTTP se simulan con HttpTestingController. */
describe('Portal conectado al backend', () => {
  let http: HttpTestingController;
  let auth: AuthService;
  let data: AsistenciaService;

  async function ingresarComoAdministrador(): Promise<void> {
    const promesa = auth.autenticar({ perfil: 'administrador', correo: 'admin@ie20191.edu.pe', contrasena: '123456' });
    http.expectOne(`${API_URL}/auth/login`).flush({
      token: 'token-de-prueba', expiraEn: Date.now() + 3_600_000,
      sesion: { usuarioId: 1, correo: 'admin@ie20191.edu.pe', nombre: 'Administrador General', perfil: 'administrador', estudianteId: null, docenteId: null, dni: null }
    });
    auth.abrirSesion(await promesa);
  }

  beforeEach(() => {
    localStorage.clear();
    localStorage.setItem('ugartino-modo', 'servidor');   // estas pruebas simulan el backend
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

    const carga = data.cargarFases();
    const peticion = http.expectOne(`${API_URL}/fases`);
    expect(peticion.request.headers.get('Authorization')).toBe('Bearer token-de-prueba');
    peticion.flush([{ semana: '01', titulo: 'Exploración', actividad: 'a', producto: 'p', estado: 'Completada' }]);
    await carga;
    expect(data.fases[0].titulo).toBe('Exploración');

    auth.signOut();
    expect(auth.isAuthenticated).toBeFalse();
  });

  it('muestra el mensaje del backend cuando el ingreso falla', async () => {
    const promesa = auth.autenticar({ perfil: 'docente', correo: 'x@ie20191.edu.pe', contrasena: 'incorrecta' });
    http.expectOne(`${API_URL}/auth/login`).flush(
      { status: 401, mensaje: 'Correo o contraseña incorrectos.', errores: ['Correo o contraseña incorrectos.'] },
      { status: 401, statusText: 'Unauthorized' });
    await expectAsync(promesa).toBeRejectedWithError('Correo o contraseña incorrectos.');
    expect(auth.isAuthenticated).toBeFalse();
  });

  it('carga la estructura académica y el padrón desde el backend', async () => {
    await ingresarComoAdministrador();
    const estructura = data.cargarEstructura();
    http.expectOne(`${API_URL}/estructura`).flush([
      { nombre: 'Inicial', grados: [{ codigo: '3 años', etiqueta: '3 años', secciones: [{ id: 1, letra: 'A', turno: 'Mañana', totalEstudiantes: 12 }] }] },
      { nombre: 'Primaria', grados: [{ codigo: '1', etiqueta: '1ro grado', secciones: [{ id: 4, letra: 'A', turno: 'Mañana', totalEstudiantes: 13 }, { id: 6, letra: 'C', turno: 'Tarde', totalEstudiantes: 12 }] }] }
    ]);
    await estructura;
    expect(data.niveles).toEqual(['Inicial', 'Primaria']);
    expect(data.gradosPorNivel.Primaria).toEqual(['1']);
    expect(data.seccionId('Primaria', '1', 'A')).toBe('4');
    expect(data.turnoSeccion('C')).toBe('Tarde');
    expect(data.etiquetaGrado('Primaria', '1')).toBe('1ro grado');

    const padron = data.cargarEstudiantes();
    http.expectOne(r => r.url === `${API_URL}/matriculas` && r.params.get('tamanio') === '1000')
      .flush({ contenido: [ESTUDIANTE], totalPaginas: 1 });
    await padron;
    expect(data.estudiantesDeAula('Primaria', '1', 'A').map(e => e.id)).toEqual(['7']);
  });

  it('guarda la asistencia en el backend y la revierte si el servidor la rechaza', async () => {
    await ingresarComoAdministrador();
    const fecha = data.hoy;

    const guardado = data.guardarEstado('7', fecha, 'falta');
    expect(data.obtenerEstado('7', fecha)).toBe('falta');
    const peticion = http.expectOne(`${API_URL}/asistencia/estudiantes/7/${fecha}`);
    expect(peticion.request.method).toBe('PUT');
    expect(peticion.request.body).toEqual({ estado: 'falta' });
    peticion.flush({ fecha, estado: 'falta' });
    expect(await guardado).toBeTrue();

    const rechazado = data.guardarEstado('7', fecha, 'presente');
    http.expectOne(`${API_URL}/asistencia/estudiantes/7/${fecha}`).flush(
      { mensaje: 'No se registra asistencia en sábado ni domingo.' }, { status: 400, statusText: 'Bad Request' });
    expect(await rechazado).toBeFalse();
    expect(data.obtenerEstado('7', fecha)).toBe('falta');
  });

  it('carga la asistencia de un rango y la consulta por estudiante y fecha', async () => {
    await ingresarComoAdministrador();
    const carga = data.cargarAsistencia('2026-10-01', '2026-10-13', '4');
    const peticion = http.expectOne(r => r.url === `${API_URL}/asistencia`);
    expect(peticion.request.params.get('seccionId')).toBe('4');
    peticion.flush([{ estudianteId: 7, fecha: '2026-10-12', estado: 'tardanza' }]);
    await carga;
    expect(data.obtenerEstado('7', '2026-10-12')).toBe('tardanza');
    expect(data.obtenerEstado('7', '2026-10-13')).toBe('pendiente');
  });

  it('al revisar una justificación toma del backend el estado resultante del día', async () => {
    await ingresarComoAdministrador();
    const fecha = '2026-10-12';
    const solicitud = data.registrarJustificacion('7', fecha, 'Cita médica');
    const creada = http.expectOne(`${API_URL}/justificaciones`);
    expect(creada.request.body).toEqual(jasmine.objectContaining({ estudianteId: 7, fecha, motivo: 'Cita médica' }));
    creada.flush({ id: 3, estudianteId: 7, estudianteNombre: 'Juan Pérez Ugarte', fecha, motivo: 'Cita médica', estado: 'Pendiente', estadoPrevio: 'falta', fechaSolicitud: '2026-10-12T18:00:00' });
    expect(await solicitud).toBeNull();
    expect(data.justificaciones[0].id).toBe(3);

    const revision = data.revisarJustificacion(data.justificaciones[0], 'Aprobada');
    http.expectOne(`${API_URL}/justificaciones/3/revision`).flush({ ...data.justificaciones[0], estudianteId: 7, estado: 'Aprobada', fechaSolicitud: '2026-10-12T18:00:00' });
    await new Promise(r => setTimeout(r));
    http.expectOne(r => r.url === `${API_URL}/estudiantes/7/asistencia`).flush([{ fecha, estado: 'justificado' }]);
    expect(await revision).toBeNull();
    expect(data.justificaciones[0].estado).toBe('Aprobada');
    expect(data.obtenerEstado('7', fecha)).toBe('justificado');
  });

  it('limita las rutas según el perfil de la sesión', async () => {
    await ingresarComoAdministrador();
    expect(auth.puedeAcceder('/administrador/matriculas')).toBeTrue();
    expect(auth.puedeAcceder('/docente/dashboard')).toBeFalse();
    expect(auth.puedeAcceder('/alumno/inicio')).toBeFalse();
  });
});
