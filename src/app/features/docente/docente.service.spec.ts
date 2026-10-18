import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, TestRequest, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { API_URL } from '../../core/config/api';
import { DocenteService } from './docente.service';

const tick = () => new Promise(resolver => setTimeout(resolver));

describe('DocenteService', () => {
  let service: DocenteService;
  let http: HttpTestingController;

  /** Responde las peticiones que vayan llegando (el adaptador las encadena) hasta que termine la carga. */
  async function responder(carga: Promise<unknown>, respuestas: [string, (r: TestRequest) => unknown][]): Promise<string[]> {
    let terminada = false;
    void carga.finally(() => terminada = true);
    const atendidas: string[] = [];
    for (let vuelta = 0; vuelta < 50 && !terminada; vuelta++) {
      await tick();
      for (const [ruta, cuerpo] of respuestas) {
        http.match(r => r.url === `${API_URL}${ruta}`).forEach(r => {
          atendidas.push(r.request.urlWithParams.replace(API_URL, ''));
          r.flush(cuerpo(r) as object);
        });
      }
    }
    await carga;
    return atendidas;
  }

  beforeEach(() => {
    localStorage.clear();
    localStorage.setItem('ugartino-sesion', JSON.stringify({
      token: 't', expiraEn: Date.now() + 3_600_000, correo: 'luis.mendoza@ie20191.edu.pe', nombre: 'Luis Mendoza Castro',
      perfil: 'docente', usuarioId: '3', docenteId: '5'
    }));
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(DocenteService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    localStorage.clear();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('arma cursos, alumnos e historial del docente con la API del backend', async () => {
    const mes = service.hoy.slice(0, 7);
    const atendidas = await responder(service.recargar(), [
      ['/docentes/5', () => ({ id: 5, nombres: 'Luis', apellidos: 'Mendoza Castro', dni: '41829306', correo: 'luis.mendoza@ie20191.edu.pe', especialidad: 'Matemática' })],
      ['/docentes/5/cursos', () => [{ id: 2, nombre: 'Matemática', gradoSeccionId: 4, aula: '1ro grado A - Primaria', docenteId: 5, docenteNombre: 'Luis Mendoza Castro', estado: 'A' }]],
      ['/grados-secciones', () => [{ id: 4, nivel: 'Primaria', grado: '1', gradoEtiqueta: '1ro grado', seccion: 'A', turno: 'Mañana', totalEstudiantes: 13, estado: 'A' }]],
      ['/estudiantes', () => [{ id: 7, nombre: 'Juan Pérez Ugarte', nombres: 'Juan', apellidos: 'Pérez Ugarte', dni: '72819201', genero: 'M', gradoSeccionId: 4, nivel: 'Primaria', grado: '1', estado: 'A' }]],
      ['/asistencia-estudiantes/grados-secciones/4/mensual', r => r.request.params.get('mes') === mes
        ? { filas: [{ estudianteId: 7, estados: { [`${mes}-01`]: 'falta', [`${mes}-02`]: 'presente', [`${mes}-05`]: 'presente' } }] }
        : { filas: [] }],
      ['/justificaciones', () => []]
    ]);

    // La asistencia por curso se pide con el cursoId (los registros diarios del aula no cuentan).
    expect(atendidas.filter(a => a.includes('/mensual')).every(a => a.includes('cursoId=2'))).toBeTrue();
    expect(service.cursos()[0]).toEqual(jasmine.objectContaining({ id: '2', codigo: 'MAT-1A', grado: '1ro', seccion: 'A', totalAlumnos: 13 }));
    expect(service.alumnosPorCurso('2')).toEqual([jasmine.objectContaining({ id: '7', porcentajeAsistencia: 67, estadoGeneral: 'Crítico' })]);
    expect(service.alumnosPorCurso('99')).toEqual([]);
    expect(service.historialSesiones().map(s => s.fecha)).toEqual([`${mes}-05`, `${mes}-02`, `${mes}-01`]);
    expect(service.perfilDocente().especialidad).toBe('Matemática');
  });

  it('aprueba una justificación en el backend y recarga la lista', async () => {
    const aprobacion = service.aprobarJustificacion('8', 'Atención justificada correctamente');
    await tick();
    const revision = http.expectOne(`${API_URL}/justificaciones/8/revision`);
    expect(revision.request.body).toEqual({ estado: 'Aprobada', observacion: 'Atención justificada correctamente' });
    revision.flush({ id: 8, estudianteId: 7 });
    await tick();
    http.expectOne(r => r.url === `${API_URL}/justificaciones`).flush([
      { id: 8, estudianteId: 7, estudianteNombre: 'Juan Pérez Ugarte', fecha: '2026-10-12', fechaSolicitud: '2026-10-12T18:00:00', motivo: 'Cita médica', estado: 'Aprobada', observacion: 'Atención justificada correctamente' }
    ]);
    expect(await aprobacion).toBeNull();
    expect(service.justificaciones()[0]).toEqual(jasmine.objectContaining({ id: '8', estado: 'Aprobada', respuestaDocente: 'Atención justificada correctamente' }));
  });

  it('devuelve el mensaje del backend si no puede observar una justificación', async () => {
    const observacion = service.rechazarJustificacion('9', 'Documento no legible');
    await tick();
    http.expectOne(`${API_URL}/justificaciones/9/revision`).flush(
      { mensaje: 'Solo el docente del curso puede revisar esta justificación.' }, { status: 403, statusText: 'Forbidden' });
    expect(await observacion).toBe('Solo el docente del curso puede revisar esta justificación.');
  });

  it('la ficha del docente la edita el administrador: el adaptador lo explica sin llamar al backend', async () => {
    const error = await service.guardarPerfil({ id: '5', nombres: 'Luis', apellidos: 'Mendoza', dni: '41829306', email: '', telefono: '', especialidad: '' });
    expect(error).toContain('administrador');
  });
});
