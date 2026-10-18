import { Injectable, signal } from '@angular/core';
import { API_URL, MODO_DATOS } from '../config/api';

export type ModoDatos = 'servidor' | 'local';

/** Clave de la versión anterior, cuando el modo se elegía en el ingreso: se borra para que no quede fijado. */
const MODO_KEY_ANTERIOR = 'ugartino-modo';
const MODO_DETECTADO_KEY = 'ugartino-modo-detectado';

/**
 * Decide de dónde salen los datos:
 * - servidor: la API del backend (Spring Boot + SQL Server).
 * - local: una API simulada dentro del navegador (sin backend), con los mismos datos de demostración.
 * Lo fija MODO_DATOS (config/api.ts); con 'auto' se usa el servidor si responde y, si no, el modo local.
 */
@Injectable({ providedIn: 'root' })
export class ModoDatosService {
  readonly modo = signal<ModoDatos>('servidor');
  /** true si se pasó al modo local porque el servidor no respondió. */
  readonly automatico = signal(false);
  /** Ya se sabe qué modo usar (las peticiones pueden salir sin esperar). */
  decidido = false;
  private resolucion: Promise<ModoDatos> | null = null;

  constructor() {
    const preferido = this.preferido();
    if (preferido) {
      this.modo.set(preferido);
      this.decidido = true;
      this.resolucion = Promise.resolve(preferido);
    }
  }

  get esLocal(): boolean { return this.modo() === 'local'; }

  /** Se resuelve una sola vez por carga de la página, antes de la primera petición. */
  resolver(): Promise<ModoDatos> {
    this.resolucion ??= this.detectar();
    return this.resolucion;
  }

  /** ¿Responde el backend? (2,5 s como máximo). */
  async servidorDisponible(): Promise<boolean> {
    const control = new AbortController();
    const espera = setTimeout(() => control.abort(), 2500);
    try {
      // Sin sesión el backend responde 401: basta con que conteste para saber que está en ejecución.
      const respuesta = await fetch(`${API_URL}/auth/sesion`, { signal: control.signal });
      return respuesta.status < 500;
    } catch {
      return false;
    } finally {
      clearTimeout(espera);
    }
  }

  private preferido(): ModoDatos | null {
    try { localStorage.removeItem(MODO_KEY_ANTERIOR); } catch { /* ignorado */ }
    if (MODO_DATOS === 'local' || MODO_DATOS === 'servidor') return MODO_DATOS;
    // Detección automática ya hecha en esta pestaña: no se vuelve a consultar al servidor en cada recarga.
    let detectado: string | null = null;
    try { detectado = sessionStorage.getItem(MODO_DETECTADO_KEY); } catch { /* ignorado */ }
    if (detectado === 'local') this.automatico.set(true);
    return detectado === 'local' || detectado === 'servidor' ? detectado : null;
  }

  private async detectar(): Promise<ModoDatos> {
    const modo: ModoDatos = await this.servidorDisponible() ? 'servidor' : 'local';
    try { sessionStorage.setItem(MODO_DETECTADO_KEY, modo); } catch { /* ignorado */ }
    this.modo.set(modo);
    this.automatico.set(modo === 'local');
    this.decidido = true;
    return modo;
  }
}
