import { Injectable, inject } from '@angular/core';
import { ApiService } from './api.service';

const SESSION_KEY = 'ugartino-sesion';

export type PerfilAcceso = 'administrador' | 'docente' | 'alumno';

/** Sesión abierta en el backend (POST /api/auth/login). Los ids se manejan como texto en el frontend. */
export interface SesionDemo {
  correo: string;
  nombre: string;
  perfil: PerfilAcceso;
  usuarioId?: string;
  estudianteId?: string;
  docenteId?: string;
  dni?: string;
}

interface SesionGuardada extends SesionDemo {
  token: string;
  expiraEn: number;
}

/** Respuesta del login lista para guardarse cuando termina la animación de ingreso. */
export interface IngresoValido {
  token: string;
  expiraEn: number;
  sesion: SesionDemo;
}

export interface DatosIngreso {
  perfil: PerfilAcceso;
  correo?: string;
  contrasena?: string;
  nombre?: string;
  dni?: string;
}

interface RespuestaLogin {
  token: string;
  expiraEn: number;
  sesion: { usuarioId: number | null; correo: string | null; nombre: string; perfil: PerfilAcceso; estudianteId: number | null; docenteId: number | null; dni: string | null };
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly api = inject(ApiService);
  /** Cada servicio con datos en memoria se registra aquí para vaciarse al cambiar de sesión. */
  private readonly reinicios: (() => void)[] = [];

  private get guardada(): SesionGuardada | null {
    try {
      const saved = localStorage.getItem(SESSION_KEY);
      const sesion = saved ? JSON.parse(saved) as SesionGuardada : null;
      return sesion && sesion.token && sesion.expiraEn > Date.now() ? sesion : null;
    } catch {
      return null;
    }
  }

  get sesion(): SesionDemo | null { return this.guardada; }
  get token(): string | null { return this.guardada?.token ?? null; }
  get isAuthenticated(): boolean { return this.guardada !== null; }

  /** Valida las credenciales en el backend sin abrir todavía la sesión en el navegador. */
  async autenticar(datos: DatosIngreso): Promise<IngresoValido> {
    const r = await this.api.post<RespuestaLogin>('/auth/login', datos);
    const texto = (valor: number | null) => valor === null || valor === undefined ? undefined : String(valor);
    return {
      token: r.token,
      expiraEn: r.expiraEn,
      sesion: {
        correo: r.sesion.correo ?? '',
        nombre: r.sesion.nombre,
        perfil: r.sesion.perfil,
        usuarioId: texto(r.sesion.usuarioId),
        estudianteId: texto(r.sesion.estudianteId),
        docenteId: texto(r.sesion.docenteId),
        dni: r.sesion.dni ?? undefined
      }
    };
  }

  abrirSesion(ingreso: IngresoValido): void {
    this.reiniciarDatos();
    localStorage.setItem(SESSION_KEY, JSON.stringify({ ...ingreso.sesion, token: ingreso.token, expiraEn: ingreso.expiraEn } satisfies SesionGuardada));
  }

  /** Actualiza el nombre mostrado (por ejemplo, tras editar "Mi perfil"). */
  actualizarNombre(nombre: string): void {
    const actual = this.guardada;
    if (actual) localStorage.setItem(SESSION_KEY, JSON.stringify({ ...actual, nombre }));
  }

  alCerrarSesion(reinicio: () => void): void {
    this.reinicios.push(reinicio);
  }

  puedeAcceder(ruta: string): boolean {
    const perfil = this.sesion?.perfil;
    if (ruta === '/administrador' || ruta.startsWith('/administrador/')) return perfil === 'administrador';
    if (ruta === '/docente' || ruta.startsWith('/docente/')) return perfil === 'docente';
    if (ruta === '/alumno' || ruta.startsWith('/alumno/')) return perfil === 'alumno';
    if (perfil === 'administrador') return ruta !== '/mi-asistencia';
    if (perfil === 'docente') return ['/dashboard', '/asistencia', '/justificaciones', '/alertas'].includes(ruta);
    if (perfil === 'alumno') return ruta === '/mi-asistencia';
    return false;
  }

  rutaInicial(): string {
    if (this.sesion?.perfil === 'administrador') return '/administrador/dashboard';
    if (this.sesion?.perfil === 'docente') return '/docente/dashboard';
    return this.sesion?.perfil === 'alumno' ? '/alumno/inicio' : '/login';
  }

  signOut(): void {
    localStorage.removeItem(SESSION_KEY);
    this.reiniciarDatos();
  }

  private reiniciarDatos(): void {
    this.reinicios.forEach(reinicio => reinicio());
  }
}
