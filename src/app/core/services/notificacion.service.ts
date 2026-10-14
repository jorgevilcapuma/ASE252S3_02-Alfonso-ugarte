import { Injectable, signal } from '@angular/core';

export type TipoNotificacion = 'exito' | 'error' | 'info' | 'aviso';

export interface Notificacion {
  id: number;
  tipo: TipoNotificacion;
  titulo: string;
  mensaje?: string;
  saliendo?: boolean;
}

/** Avisos flotantes (toasts) compartidos por todas las pantallas. */
@Injectable({ providedIn: 'root' })
export class NotificacionService {
  private siguienteId = 1;
  readonly avisos = signal<Notificacion[]>([]);

  exito(titulo: string, mensaje?: string): void { this.mostrar('exito', titulo, mensaje); }
  error(titulo: string, mensaje?: string): void { this.mostrar('error', titulo, mensaje); }
  info(titulo: string, mensaje?: string): void { this.mostrar('info', titulo, mensaje); }
  aviso(titulo: string, mensaje?: string): void { this.mostrar('aviso', titulo, mensaje); }

  cerrar(id: number): void {
    this.avisos.update(lista => lista.map(aviso => aviso.id === id ? { ...aviso, saliendo: true } : aviso));
    setTimeout(() => this.avisos.update(lista => lista.filter(aviso => aviso.id !== id)), 260);
  }

  private mostrar(tipo: TipoNotificacion, titulo: string, mensaje?: string, duracion = 4200): void {
    const id = this.siguienteId++;
    this.avisos.update(lista => [...lista.slice(-3), { id, tipo, titulo, mensaje }]);
    setTimeout(() => this.cerrar(id), duracion);
  }
}
