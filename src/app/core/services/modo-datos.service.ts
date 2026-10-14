import { Injectable, signal } from '@angular/core';
import { API_URL, MODO_DATOS } from '../config/api';

export type ModoDatos = 'servidor' | 'local';

const MODO_KEY = 'ugartino-modo';
const MODO_DETECTADO_KEY = 'ugartino-modo-detectado';

/**
 * Decide de dónde salen los datos:
 * - servidor: la API del backend (Spring Boot + SQL Server).
 * - local: una API simulada dentro del navegador (sin backend), con los mismos datos de demostración.
 * Si no hay un modo elegido, se usa el servidor cuando responde y, si no, el modo local.
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

  /** Cambio manual desde la pantalla de ingreso (se recuerda en este navegador). */
  elegir(modo: ModoDatos): void {
    try { localStorage.setItem(MODO_KEY, modo); } catch { /* sin almacenamiento: solo esta pestaña */ }
    this.modo.set(modo);
    this.automatico.set(false);
    this.decidido = true;
    this.resolucion = Promise.resolve(modo);
  }

  /** ¿Responde el backend? (2,5 s como máximo). */
  async servidorDisponible(): Promise<boolean> {
    const control = new AbortController();
    const espera = setTimeout(() => control.abort(), 2500);
    try {
      const respuesta = await fetch(`${API_URL}/institucion`, { signal: control.signal });
      return respuesta.ok;
    } catch {
      return false;
    } finally {
      clearTimeout(espera);
    }
  }

  private preferido(): ModoDatos | null {
    let guardado: string | null = null;
    try { guardado = localStorage.getItem(MODO_KEY); } catch { /* ignorado */ }
    const valor = guardado ?? MODO_DATOS;
    if (valor === 'local' || valor === 'servidor') return valor;
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
