import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { API_URL } from '../../core/config/api';
import { DocenteService } from './docente.service';

describe('DocenteService', () => {
  let service: DocenteService;
  let http: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    localStorage.setItem('ugartino-modo', 'servidor');
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

  it('carga cursos y alumnos del docente desde el backend', async () => {
    const carga = service.recargar();
    http.expectOne(`${API_URL}/comunicados`).flush([]);
    http.expectOne(`${API_URL}/docentes/5`).flush({ id: 5, nombres: 'Luis', apellidos: 'Mendoza Castro', dni: '41829306', email: 'luis.mendoza@ie20191.edu.pe', telefono: null, especialidad: 'Matemática' });
    http.expectOne(`${API_URL}/docentes/5/cursos`).flush([
      { id: 2, codigo: 'MAT-1A', nombre: 'Matemática', nivel: 'Primaria', grado: '1', seccion: 'A', horario: 'Lunes 09:15 - 10:30', aula: 'Aula 04', totalAlumnos: 13 }
    ]);
    http.expectOne(`${API_URL}/docentes/5/estudiantes`).flush([
      { id: 7, dni: '72819201', nombres: 'Juan', apellidos: 'Pérez Ugarte', genero: 'M', nivel: 'Primaria', gradoSeccion: '1ro grado A', cursoId: 2, curso: 'Matemática', cursoNombre: 'Matemática (1ro grado A)', sesiones: 3, porcentajeAsistencia: 67, estadoGeneral: 'Crítico' }
    ]);
    http.expectOne(`${API_URL}/docentes/5/historial`).flush([]);
    http.expectOne(r => r.url === `${API_URL}/justificaciones` && r.params.get('docenteId') === '5').flush([]);
    await carga;

    expect(service.cursos()[0]).toEqual(jasmine.objectContaining({ id: '2', grado: '1ro', seccion: 'A' }));
    expect(service.alumnosPorCurso('2').map(a => a.id)).toEqual(['7']);
    expect(service.alumnosPorCurso('99')).toEqual([]);
    expect(service.perfilDocente().especialidad).toBe('Matemática');
  });

  it('aprueba una justificación en el backend y recarga la lista', async () => {
    const aprobacion = service.aprobarJustificacion('8', 'Atención justificada correctamente');
    const revision = http.expectOne(`${API_URL}/justificaciones/8/revision`);
    expect(revision.request.body).toEqual({ estado: 'Aprobada', observacion: 'Atención justificada correctamente' });
    revision.flush({});
    await new Promise(r => setTimeout(r));
    http.expectOne(r => r.url === `${API_URL}/justificaciones`).flush([
      { id: 8, estudianteId: 7, estudianteNombre: 'Juan Pérez Ugarte', fecha: '2026-10-12', fechaSolicitud: '2026-10-12T18:00:00', motivo: 'Cita médica', estado: 'Aprobada', observacion: 'Atención justificada correctamente' }
    ]);
    expect(await aprobacion).toBeNull();
    expect(service.justificaciones()[0]).toEqual(jasmine.objectContaining({ id: '8', estado: 'Aprobada', respuestaDocente: 'Atención justificada correctamente' }));
  });

  it('devuelve el mensaje del backend si no puede observar una justificación', async () => {
    const observacion = service.rechazarJustificacion('9', 'Documento no legible');
    http.expectOne(`${API_URL}/justificaciones/9/revision`).flush(
      { mensaje: 'Solo el docente del curso puede revisar esta justificación.' }, { status: 403, statusText: 'Forbidden' });
    expect(await observacion).toBe('Solo el docente del curso puede revisar esta justificación.');
  });
});
