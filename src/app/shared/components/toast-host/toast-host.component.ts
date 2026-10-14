import { Component, inject } from '@angular/core';
import { NotificacionService, TipoNotificacion } from '../../../core/services/notificacion.service';
import { IconoComponent, NombreIcono } from '../icono/icono.component';

@Component({
  selector: 'app-toast-host',
  standalone: true,
  imports: [IconoComponent],
  template: `
    <div class="toast-stack" aria-live="polite">
      @for (aviso of notificaciones.avisos(); track aviso.id) {
        <div [class]="'toast-card is-' + aviso.tipo" [class.is-leaving]="aviso.saliendo" role="status">
          <span class="toast-icon"><app-icono [nombre]="iconos[aviso.tipo]" [tamano]="18" /></span>
          <div class="toast-copy">
            <strong>{{ aviso.titulo }}</strong>
            @if (aviso.mensaje) { <p>{{ aviso.mensaje }}</p> }
          </div>
          <button type="button" aria-label="Cerrar aviso" (click)="notificaciones.cerrar(aviso.id)">×</button>
          <i class="toast-timer"></i>
        </div>
      }
    </div>
  `,
  styles: [`
    .toast-stack { position: fixed; z-index: 100; right: 24px; bottom: 24px; display: grid; gap: 10px; width: min(380px, calc(100vw - 32px)); pointer-events: none; }
    .toast-card { position: relative; display: flex; align-items: flex-start; gap: 12px; overflow: hidden; padding: 14px 14px 16px; border: 1px solid var(--color-line); border-radius: var(--radius-md); background: #fff; box-shadow: 0 18px 40px rgba(28, 34, 51, .16); pointer-events: auto; animation: toast-enter .42s cubic-bezier(.2, 1.2, .3, 1) both; }
    .toast-card.is-leaving { animation: toast-leave .26s ease forwards; }
    .toast-icon { display: grid; width: 34px; height: 34px; flex: 0 0 34px; place-items: center; border-radius: 50%; }
    .toast-copy { flex: 1; min-width: 0; }
    .toast-copy strong { display: block; color: var(--color-ink); font-size: 14px; }
    .toast-copy p { margin: 2px 0 0; color: var(--color-muted); font-size: 13px; line-height: 1.4; }
    button { width: 28px; height: 28px; flex: 0 0 28px; border: 0; border-radius: 50%; color: var(--color-muted); background: transparent; font-size: 20px; line-height: 1; }
    button:hover { color: var(--color-ink); background: var(--color-surface-2); }
    .toast-timer { position: absolute; left: 0; bottom: 0; width: 100%; height: 3px; transform-origin: left; animation: toast-timer 4.2s linear forwards; }
    .is-exito .toast-icon { color: var(--color-success); background: var(--color-success-soft); } .is-exito .toast-timer { background: var(--color-success); }
    .is-error .toast-icon { color: var(--color-danger); background: var(--color-danger-soft); } .is-error .toast-timer { background: var(--color-danger); }
    .is-info .toast-icon { color: var(--color-info); background: var(--color-info-soft); } .is-info .toast-timer { background: var(--color-info); }
    .is-aviso .toast-icon { color: var(--color-warning); background: var(--color-warning-soft); } .is-aviso .toast-timer { background: var(--color-gold); }
    @keyframes toast-enter { from { opacity: 0; transform: translateX(40px) scale(.95); } to { opacity: 1; transform: none; } }
    @keyframes toast-leave { to { opacity: 0; transform: translateX(40px); } }
    @keyframes toast-timer { from { transform: scaleX(1); } to { transform: scaleX(0); } }
    @media (max-width: 640px) { .toast-stack { right: 16px; bottom: 16px; } }
  `]
})
export class ToastHostComponent {
  readonly notificaciones = inject(NotificacionService);
  readonly iconos: Record<TipoNotificacion, NombreIcono> = { exito: 'check', error: 'alerta', info: 'info', aviso: 'campana' };
}
