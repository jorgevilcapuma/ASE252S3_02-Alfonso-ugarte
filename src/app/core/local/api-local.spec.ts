import { ApiLocal } from './api-local';

/** Modo local: el frontend funciona sin backend con las mismas rutas y reglas. */
describe('ApiLocal (modo sin servidor)', () => {
  let api: ApiLocal;

  const hoy = () => {
    const d = new Date();
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
  };
  /** El n-ésimo día hábil anterior a hoy (el 1.° ya tiene una justificación pendiente en los datos de demostración). */
  const diaHabilAnterior = (n = 2) => {
    const d = new Date();
    for (let i = 0; i < n; i++) do { d.setDate(d.getDate() - 1); } while ([0, 6].includes(d.getDay()));
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
  };

  async function ingresar(cuerpo: Record<string, unknown>) {
    const r = await api.atender('POST', '/auth/login', {}, cuerpo) as { token: string; expiraEn: number; sesion: Record<string, unknown> };
    const texto = (v: unknown) => v === null || v === undefined ? undefined : String(v);
    localStorage.setItem('ugartino-sesion', JSON.stringify({
      ...r.sesion, usuarioId: texto(r.sesion['usuarioId']), estudianteId: texto(r.sesion['estudianteId']), docenteId: texto(r.sesion['docenteId']),
      token: r.token, expiraEn: r.expiraEn
    }));
    return r.sesion;
  }

  beforeEach(() => {
    localStorage.clear();
    api = new ApiLocal();
  });

  afterEach(() => localStorage.clear());

  it('siembra los mismos datos que el backend', async () => {
    await ingresar({ perfil: 'administrador', correo: 'admin@ie20191.edu.pe', contrasena: '123456' });
    const matriculas = await api.atender('GET', '/matriculas') as { contenido: unknown[] };
    const estructura = await api.atender('GET', '/estructura') as { grados: unknown[] }[];
    const usuarios = await api.atender('GET', '/usuarios') as unknown[];
    expect(matriculas.contenido.length).toBe(505);
    expect(estructura.map(n => n.grados.length)).toEqual([3, 6, 5]);
    expect(usuarios.length).toBe(9);
  });

  it('valida el ingreso igual que el backend', async () => {
    await expectAsync(api.atender('POST', '/auth/login', {}, { perfil: 'administrador', correo: 'admin@ie20191.edu.pe', contrasena: 'otra' }))
      .toBeRejectedWithError('Correo o contraseña incorrectos.');
    await expectAsync(api.atender('POST', '/auth/login', {}, { perfil: 'alumno', correo: 'juan.alvarado@ie20191.edu.pe', contrasena: '123456' }))
      .toBeRejectedWithError('Esta cuenta está desactivada. Comunícate con el administrador.');
    const alumno = await ingresar({ perfil: 'alumno', nombre: 'juan perez ugarte', dni: '72819201' });
    expect(alumno['nombre']).toBe('Juan Pérez Ugarte');
  });

  it('matricula, evita DNI duplicados y guarda los datos en el navegador', async () => {
    await ingresar({ perfil: 'administrador', correo: 'admin@ie20191.edu.pe', contrasena: '123456' });
    const nuevo = await api.atender('POST', '/matriculas', {}, { nombre: 'Ana Lucía Torres Vega', dni: '76543210', genero: 'F', nivel: 'Primaria', grado: '2', seccion: 'B' }) as Record<string, unknown>;
    expect(nuevo['apellidos']).toBe('Torres Vega');
    await expectAsync(api.atender('POST', '/matriculas', {}, { nombre: 'Ana Lucía Torres Vega', dni: '76543210', nivel: 'Primaria', grado: '2', seccion: 'B' }))
      .toBeRejectedWithError('Ya existe un estudiante matriculado con ese DNI.');
    const recargada = new ApiLocal();   // como recargar la página
    const lista = await recargada.atender('GET', '/matriculas') as { contenido: { dni: string }[] };
    expect(lista.contenido.some(e => e.dni === '76543210')).toBeTrue();
  });

  it('registra asistencia y aplica la revisión de justificaciones', async () => {
    await ingresar({ perfil: 'administrador', correo: 'admin@ie20191.edu.pe', contrasena: '123456' });
    const juan = (await api.atender('GET', '/matriculas') as { contenido: { id: number; dni: string }[] }).contenido.find(e => e.dni === '72819201')!;
    const dia = diaHabilAnterior();
    await api.atender('PUT', `/asistencia/estudiantes/${juan.id}/${dia}`, {}, { estado: 'tardanza' });
    const manana = new Date(); manana.setDate(manana.getDate() + 1);
    await expectAsync(api.atender('PUT', `/asistencia/estudiantes/${juan.id}/${manana.toISOString().slice(0, 10)}`, {}, { estado: 'falta' }))
      .toBeRejected();

    await ingresar({ perfil: 'alumno', nombre: 'Juan Pérez Ugarte', dni: '72819201' });
    const sol = await api.atender('POST', '/justificaciones', {}, { fecha: dia, motivo: 'Transporte' }) as { id: number; estadoPrevio: string };
    expect(sol.estadoPrevio).toBe('tardanza');
    await expectAsync(api.atender('POST', '/justificaciones', {}, { fecha: dia, motivo: 'Otra' })).toBeRejectedWithError(/pendiente/);

    await ingresar({ perfil: 'administrador', correo: 'admin@ie20191.edu.pe', contrasena: '123456' });
    await api.atender('PATCH', `/justificaciones/${sol.id}/revision`, {}, { estado: 'Observada', observacion: 'Sin sustento' });
    const historial = await api.atender('GET', `/estudiantes/${juan.id}/asistencia`, { desde: dia, hasta: dia }) as { estado: string }[];
    expect(historial[0].estado).toBe('tardanza');
    expect(hoy() >= dia).toBeTrue();
  });

  it('el docente solo gestiona sus cursos y guarda sesiones', async () => {
    await ingresar({ perfil: 'administrador', correo: 'admin@ie20191.edu.pe', contrasena: '123456' });
    const juan = (await api.atender('GET', '/matriculas') as { contenido: { id: number; dni: string }[] }).contenido.find(e => e.dni === '72819201')!;
    const deElena = (await api.atender('GET', `/alumnos/${juan.id}/cursos`) as { id: number; codigo: string }[]).find(c => c.codigo === 'COM-1A')!;

    const sesion = await ingresar({ perfil: 'docente', correo: 'luis.mendoza@ie20191.edu.pe', contrasena: '123456' });
    const cursos = await api.atender('GET', `/docentes/${sesion['docenteId']}/cursos`) as { id: number; codigo: string }[];
    expect(cursos.map(c => c.codigo)).toEqual(['MAT-1A']);
    const dia = diaHabilAnterior();
    const lista = await api.atender('GET', `/asignaciones/${cursos[0].id}/sesiones/${dia}`) as { filas: { alumnoId: number }[] };
    const registros = lista.filas.map((f, i) => ({ estudianteId: f.alumnoId, estado: i === 0 ? 'falta' : 'asistio' }));
    const guardada = await api.atender('PUT', `/asignaciones/${cursos[0].id}/sesiones/${dia}`, {}, { registros }) as { yaRegistrado: boolean };
    expect(guardada.yaRegistrado).toBeTrue();
    const otro = await api.atender('GET', '/comunicados') as unknown[];
    expect(otro).toEqual([]);
    await expectAsync(api.atender('GET', `/asignaciones/${deElena.id}/sesiones/${dia}`)).toBeRejectedWithError('Este curso no está asignado a tu cuenta.');
  });
});
