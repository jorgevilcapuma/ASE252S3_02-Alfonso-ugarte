import * as R from '../../../core/validacion/reglas';
import { NombreIcono } from '../../../shared/components/icono/icono.component';

/** Fila tal como la devuelve el backend. */
export type Registro = Record<string, unknown> & { id: number; estado: string };
export type Formulario = Record<string, string>;
export type Opciones = Record<'aulas' | 'docentes', { valor: string; etiqueta: string }[]>;

export interface CampoGestion {
  clave: string;
  etiqueta: string;
  tipo: 'texto' | 'email' | 'fecha' | 'select';
  requerido?: boolean;
  ancho?: boolean;
  ayuda?: string;
  maximo?: number;
  /** Regla del campo (core/validacion/reglas.ts): devuelve el mensaje de error o null. */
  validar?: (valor: string, formulario: Formulario, hoy: string) => string | null;
  /** Opciones fijas o las que se cargan del backend (aulas o docentes activos). */
  opciones?: { valor: string; etiqueta: string }[] | keyof Opciones;
}

export interface ConfigGestion {
  titulo: string;
  singular: string;
  /** Texto del botón de alta ('Nuevo curso', 'Nueva aula'). */
  nuevo: string;
  descripcion: string;
  icono: NombreIcono;
  /** Ruta del CRUD en el backend: GET/POST ruta, PUT ruta/{id}, PATCH ruta/{id}/eliminar|restaurar. */
  ruta: string;
  columnas: { etiqueta: string; principal: (r: Registro) => string; secundario?: (r: Registro) => string }[];
  campos: CampoGestion[];
  /** Texto en el que busca el cuadro de búsqueda. */
  buscar: (r: Registro) => string;
  /** Datos que necesita del backend para sus listas desplegables. */
  necesita: (keyof Opciones)[];
  /** Ficha con cuenta de acceso (su correo es el usuario del portal): docentes y estudiantes. */
  conCuenta?: { perfil: string; sinCorreo: string };
  /** Mensaje tras guardar cuando la ficha no usa cuenta de acceso (estudiantes). */
  mensajeGuardado?: (r: Registro) => string;
  /** Cuerpo del POST/PUT a partir del formulario. */
  cuerpo: (f: Formulario) => Record<string, unknown>;
}

const txt = (valor: unknown) => (valor ?? '') as string;
const vacio = (valor: string) => valor.trim() === '' ? null : valor.trim();
const numero = (valor: string) => valor ? Number(valor) : null;

export const GESTION: Record<string, ConfigGestion> = {
  docentes: {
    titulo: 'Docentes y personal',
    singular: 'personal',
    nuevo: 'Nuevo personal',
    descripcion: 'Docentes, auxiliares, administrativos, directivos y personal de servicio. Solo el cargo Docente dicta cursos.',
    icono: 'usuarios',
    ruta: '/docentes',
    columnas: [
      { etiqueta: 'PERSONAL', principal: r => txt(r['nombreCompleto']), secundario: r => `DNI ${txt(r['dni'])}` },
      { etiqueta: 'CARGO', principal: r => txt(r['cargo']), secundario: r => txt(r['especialidad']) || txt(r['area']) },
      { etiqueta: 'ACCESO AL PORTAL', principal: r => r['usuarioId'] ? txt(r['correo']) : 'Sin cuenta',
        secundario: r => r['usuarioId'] ? 'Ingresa con este correo' : txt(r['cargo']) === 'Docente' ? 'Agrega su correo para crearla' : '' },
      { etiqueta: 'CELULAR', principal: r => txt(r['celular']) || '—' }
    ],
    campos: [
      { clave: 'nombres', etiqueta: 'Nombres', tipo: 'texto', requerido: true, maximo: 80, validar: v => R.nombre(v, 'Los nombres') },
      { clave: 'apellidos', etiqueta: 'Apellidos', tipo: 'texto', requerido: true, maximo: 80, validar: v => R.nombre(v, 'Los apellidos') },
      { clave: 'dni', etiqueta: 'DNI', tipo: 'texto', requerido: true, maximo: 8, ayuda: '8 dígitos', validar: R.dni },
      { clave: 'cargo', etiqueta: 'Cargo', tipo: 'select', requerido: true,
        opciones: ['Docente', 'Auxiliar', 'Administrativo', 'Directivo', 'Servicio'].map(c => ({ valor: c, etiqueta: c })) },
      { clave: 'sexo', etiqueta: 'Sexo', tipo: 'select', requerido: true, opciones: [{ valor: 'F', etiqueta: 'Femenino' }, { valor: 'M', etiqueta: 'Masculino' }] },
      { clave: 'fechaIngreso', etiqueta: 'Fecha de ingreso', tipo: 'fecha', validar: (v, _, hoy) => R.fechaNoFutura(v, hoy) },
      { clave: 'especialidad', etiqueta: 'Especialidad', tipo: 'texto', maximo: 120 },
      { clave: 'area', etiqueta: 'Área', tipo: 'texto', maximo: 80 },
      { clave: 'celular', etiqueta: 'Celular', tipo: 'texto', maximo: 9, ayuda: '9 dígitos, empieza con 9', validar: R.celularOpcional },
      { clave: 'correo', etiqueta: 'Correo (opcional)', tipo: 'email', maximo: 120, ancho: true, validar: R.correoOpcional,
        ayuda: 'Con cargo Docente, este correo crea su cuenta para ingresar (contraseña inicial 123456). No hace falta crearla en Usuarios.' }
    ],
    buscar: r => `${txt(r['nombreCompleto'])} ${txt(r['dni'])} ${txt(r['cargo'])} ${txt(r['especialidad'])}`,
    necesita: [],
    conCuenta: { perfil: 'Docente', sinCorreo: 'Sin correo no tendrá cuenta para ingresar al portal.' },
    cuerpo: f => ({
      nombres: f['nombres'], apellidos: f['apellidos'], dni: f['dni'].trim(), cargo: f['cargo'], sexo: f['sexo'],
      fechaIngreso: vacio(f['fechaIngreso']), especialidad: vacio(f['especialidad']), area: vacio(f['area']),
      celular: vacio(f['celular']), correo: vacio(f['correo'])
    })
  },

  estudiantes: {
    titulo: 'Estudiantes',
    singular: 'estudiante',
    nuevo: 'Nuevo estudiante',
    descripcion: 'Padrón de estudiantes por aula. Ingresan al portal con su nombre completo y DNI. Eliminar lo retira sin borrar su historial; se puede restaurar.',
    icono: 'alumnos',
    ruta: '/estudiantes',
    columnas: [
      { etiqueta: 'ESTUDIANTE', principal: r => txt(r['nombre']), secundario: r => `DNI ${txt(r['dni'])}` },
      { etiqueta: 'AULA', principal: r => `${txt(r['gradoEtiqueta'])} ${txt(r['seccion'])}`, secundario: r => `${txt(r['nivel'])} · ${txt(r['turno'])}` },
      { etiqueta: 'CONTACTO', principal: r => txt(r['telefono']) || '—', secundario: r => txt(r['correo']) }
    ],
    campos: [
      { clave: 'nombres', etiqueta: 'Nombres', tipo: 'texto', requerido: true, maximo: 80, validar: v => R.nombre(v, 'Los nombres') },
      { clave: 'apellidos', etiqueta: 'Apellidos', tipo: 'texto', requerido: true, maximo: 80, validar: v => R.nombre(v, 'Los apellidos') },
      { clave: 'dni', etiqueta: 'DNI', tipo: 'texto', requerido: true, maximo: 8, ayuda: '8 dígitos', validar: R.dni },
      { clave: 'gradoSeccionId', etiqueta: 'Grado y sección', tipo: 'select', requerido: true, opciones: 'aulas' },
      { clave: 'genero', etiqueta: 'Género', tipo: 'select', opciones: [{ valor: '', etiqueta: 'Sin indicar' }, { valor: 'F', etiqueta: 'Femenino' }, { valor: 'M', etiqueta: 'Masculino' }] },
      { clave: 'telefono', etiqueta: 'Teléfono', tipo: 'texto', maximo: 20, validar: R.telefonoOpcional },
      { clave: 'correo', etiqueta: 'Correo (opcional)', tipo: 'email', maximo: 120, ancho: true, validar: R.correoOpcional,
        ayuda: 'Solo como dato de contacto. El alumno ingresa al portal con su nombre completo y su DNI.' }
    ],
    buscar: r => `${txt(r['nombre'])} ${txt(r['dni'])} ${txt(r['gradoEtiqueta'])} ${txt(r['seccion'])} ${txt(r['nivel'])}`,
    necesita: ['aulas'],
    mensajeGuardado: r => `Ingresa al portal como Alumno con su nombre completo (${txt(r['nombre'])}) y su DNI ${txt(r['dni'])}.`,
    cuerpo: f => ({
      nombres: f['nombres'], apellidos: f['apellidos'], dni: f['dni'].trim(), gradoSeccionId: numero(f['gradoSeccionId']),
      genero: vacio(f['genero']), telefono: vacio(f['telefono']), correo: vacio(f['correo'])
    })
  },

  cursos: {
    titulo: 'Cursos',
    singular: 'curso',
    nuevo: 'Nuevo curso',
    descripcion: 'Cada curso se dicta en un aula por un docente activo. Eliminar lo oculta sin borrar su asistencia.',
    icono: 'cursos',
    ruta: '/cursos',
    columnas: [
      { etiqueta: 'CURSO', principal: r => txt(r['nombre']), secundario: r => txt(r['aula']) },
      { etiqueta: 'DOCENTE', principal: r => txt(r['docenteNombre']) }
    ],
    campos: [
      { clave: 'nombre', etiqueta: 'Nombre del curso', tipo: 'texto', requerido: true, maximo: 120, ancho: true, validar: v => R.texto(v, 3, 'El nombre del curso') },
      { clave: 'gradoSeccionId', etiqueta: 'Grado y sección', tipo: 'select', requerido: true, opciones: 'aulas' },
      { clave: 'docenteId', etiqueta: 'Docente', tipo: 'select', requerido: true, opciones: 'docentes' }
    ],
    buscar: r => `${txt(r['nombre'])} ${txt(r['aula'])} ${txt(r['docenteNombre'])}`,
    necesita: ['aulas', 'docentes'],
    cuerpo: f => ({ nombre: f['nombre'], gradoSeccionId: numero(f['gradoSeccionId']), docenteId: numero(f['docenteId']) })
  },

  'grados-secciones': {
    titulo: 'Grados y secciones',
    singular: 'aula',
    nuevo: 'Nueva aula',
    descripcion: 'Aulas por nivel, grado y sección. Solo se elimina un aula sin estudiantes ni cursos activos.',
    icono: 'estructura',
    ruta: '/grados-secciones',
    columnas: [
      { etiqueta: 'AULA', principal: r => `${txt(r['gradoEtiqueta'])} ${txt(r['seccion'])}`, secundario: r => txt(r['nivel']) },
      { etiqueta: 'TURNO', principal: r => txt(r['turno']) },
      { etiqueta: 'ESTUDIANTES', principal: r => String(r['totalEstudiantes'] ?? 0) }
    ],
    campos: [
      { clave: 'nivel', etiqueta: 'Nivel', tipo: 'select', requerido: true, opciones: ['Inicial', 'Primaria', 'Secundaria'].map(n => ({ valor: n, etiqueta: n })) },
      { clave: 'grado', etiqueta: 'Grado', tipo: 'texto', requerido: true, maximo: 10, ayuda: 'Inicial: 3 a 5 años · Primaria: 1 a 6 · Secundaria: 1 a 5', validar: (v, f) => R.gradoEnNivel(f['nivel'], v) },
      { clave: 'seccion', etiqueta: 'Sección', tipo: 'texto', requerido: true, maximo: 1, ayuda: 'Una letra: A, B, C…', validar: R.seccion },
      { clave: 'turno', etiqueta: 'Turno', tipo: 'select',
        opciones: [{ valor: '', etiqueta: 'Automático (la C es de tarde)' }, { valor: 'Mañana', etiqueta: 'Mañana' }, { valor: 'Tarde', etiqueta: 'Tarde' }] }
    ],
    buscar: r => `${txt(r['nombre'])} ${txt(r['turno'])}`,
    necesita: [],
    cuerpo: f => ({ nivel: f['nivel'], grado: f['grado'].trim(), seccion: f['seccion'].trim().toUpperCase(), turno: vacio(f['turno']) })
  }
};
