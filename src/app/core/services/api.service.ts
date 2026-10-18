import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { API_URL } from '../config/api';
import { ApiLocal } from '../local/api-local';
import { ApiServidor } from '../servidor/api-servidor';
import { ErrorApi } from './error-api';
import { ModoDatosService } from './modo-datos.service';

export { ErrorApi } from './error-api';

type Parametros = Record<string, string | number | boolean | null | undefined>;
type Metodo = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';

/**
 * Acceso a los datos con promesas: las pantallas usan async/await y muestran `error.message`.
 * Según el modo, la petición va al backend (servidor) o a la API simulada del navegador (local).
 */
@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);
  private readonly modo = inject(ModoDatosService);
  private local: ApiLocal | null = null;
  private servidor: ApiServidor | null = null;

  get<T>(ruta: string, parametros?: Parametros): Promise<T> {
    return this.solicitar('GET', ruta, undefined, parametros);
  }

  post<T>(ruta: string, cuerpo: unknown = {}): Promise<T> {
    return this.solicitar('POST', ruta, cuerpo);
  }

  put<T>(ruta: string, cuerpo: unknown = {}): Promise<T> {
    return this.solicitar('PUT', ruta, cuerpo);
  }

  patch<T>(ruta: string, cuerpo: unknown = {}): Promise<T> {
    return this.solicitar('PATCH', ruta, cuerpo);
  }

  delete<T = void>(ruta: string): Promise<T> {
    return this.solicitar('DELETE', ruta);
  }

  /** Vuelve a los datos de demostración del modo local. */
  reiniciarDatosLocales(): void {
    this.apiLocal().reiniciar();
  }

  /** Texto del error para mostrar al usuario. */
  static mensaje(error: unknown): string {
    return error instanceof Error ? error.message : 'Ocurrió un error inesperado.';
  }

  private solicitar<T>(metodo: Metodo, ruta: string, cuerpo?: unknown, parametros?: Parametros): Promise<T> {
    // Con el modo ya decidido la petición sale de inmediato; si no, se espera a saber si el servidor responde.
    if (this.modo.decidido) return this.enviar(metodo, ruta, cuerpo, parametros);
    return this.modo.resolver().then(() => this.enviar<T>(metodo, ruta, cuerpo, parametros));
  }

  /**
   * Las pantallas usan un mismo conjunto de rutas: en modo local las atiende la API simulada del navegador y en modo
   * servidor el adaptador las traduce a la API del backend.
   */
  private enviar<T>(metodo: Metodo, ruta: string, cuerpo?: unknown, parametros?: Parametros): Promise<T> {
    const params = Object.fromEntries(Object.entries(parametros ?? {})
      .filter(([, valor]) => valor !== null && valor !== undefined && valor !== '')
      .map(([clave, valor]) => [clave, String(valor)]));
    const destino = this.modo.esLocal ? this.apiLocal() : this.apiServidor();
    return destino.atender(metodo, ruta, params, (cuerpo ?? {}) as Record<string, unknown>) as Promise<T>;
  }

  /** Petición HTTP directa al backend (la usa el adaptador del modo servidor). */
  private backend<T>(metodo: Metodo, ruta: string, parametros?: Parametros, cuerpo?: unknown): Promise<T> {
    const url = API_URL + ruta;
    const peticion = metodo === 'GET' ? this.http.get<T>(url, { params: this.params(parametros) })
      : metodo === 'POST' ? this.http.post<T>(url, cuerpo)
        : metodo === 'PUT' ? this.http.put<T>(url, cuerpo)
          : metodo === 'PATCH' ? this.http.patch<T>(url, cuerpo)
            : this.http.delete<T>(url);
    return this.ejecutar(peticion);
  }

  private apiLocal(): ApiLocal {
    this.local ??= new ApiLocal();
    return this.local;
  }

  private apiServidor(): ApiServidor {
    this.servidor ??= new ApiServidor((metodo, ruta, parametros, cuerpo) => this.backend(metodo, ruta, parametros, cuerpo));
    return this.servidor;
  }

  private async ejecutar<T>(peticion: ReturnType<HttpClient['get']>): Promise<T> {
    try {
      return await firstValueFrom(peticion) as T;
    } catch (error) {
      throw this.convertir(error);
    }
  }

  private convertir(error: unknown): ErrorApi {
    if (error instanceof HttpErrorResponse) {
      if (error.status === 0) {
        return new ErrorApi(0, 'No se pudo conectar con el servidor. Verifica que el backend esté en ejecución (http://localhost:8080) e intenta de nuevo.');
      }
      const cuerpo = error.error as { mensaje?: string; errores?: string[] } | null;
      const errores = cuerpo?.errores ?? [];
      const mensaje = errores.length > 1 ? errores.join(' ') : cuerpo?.mensaje ?? `Error ${error.status} del servidor.`;
      return new ErrorApi(error.status, mensaje, errores);
    }
    return new ErrorApi(-1, ApiService.mensaje(error));
  }

  private params(parametros?: Parametros): HttpParams {
    let params = new HttpParams();
    for (const [clave, valor] of Object.entries(parametros ?? {})) {
      if (valor !== null && valor !== undefined && valor !== '') params = params.set(clave, String(valor));
    }
    return params;
  }
}
