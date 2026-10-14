import { Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';
import { AsistenciaService } from './core/services/asistencia.service';
import { AuthService } from './core/services/auth.service';
import { TemaService } from './core/services/tema.service';
import { ShellComponent } from './layout/shell/shell.component';
import { PantallaCargaComponent } from './shared/components/pantalla-carga/pantalla-carga.component';
import { ToastHostComponent } from './shared/components/toast-host/toast-host.component';

const SPLASH_KEY = 'ugartino-splash-visto';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, ShellComponent, ToastHostComponent, PantallaCargaComponent],
  template: `
    @if (mostrarShell) { <app-shell /> } @else { <router-outlet /> }
    <app-toast-host />
    @if (splash()) { <app-pantalla-carga variante="inicio" [saliendo]="splashSaliendo()" /> }
  `
})
export class AppComponent {
  readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  /** Aplica los colores institucionales guardados antes de pintar la primera pantalla. */
  private readonly tema = inject(TemaService);

  /** Ruta actual: el login nunca se dibuja dentro del menú del portal. */
  private readonly enLogin = signal(typeof location === 'undefined' || location.pathname.startsWith('/login'));

  /** La presentación se muestra una vez por pestaña, no en cada recarga. */
  readonly splash = signal(this.debeMostrarSplash());
  readonly splashSaliendo = signal(false);

  constructor() {
    // Nombre del colegio e identidad visual desde el backend (también los usa la pantalla de ingreso).
    void inject(AsistenciaService).cargarInstitucion();

    this.router.events.pipe(filter((evento): evento is NavigationEnd => evento instanceof NavigationEnd), takeUntilDestroyed()).subscribe(evento => {
      const enLogin = evento.urlAfterRedirects.split(/[?#]/)[0] === '/login';
      this.enLogin.set(enLogin);
      // Con sesión abierta no tiene sentido quedarse en el login: se va al panel del perfil.
      if (enLogin && this.auth.isAuthenticated) void this.router.navigateByUrl(this.auth.rutaInicial(), { replaceUrl: true });
    });

    if (!this.splash()) return;
    setTimeout(() => this.splashSaliendo.set(true), 1300);
    setTimeout(() => this.splash.set(false), 1650);
  }

  get mostrarShell(): boolean { return this.auth.isAuthenticated && !this.enLogin(); }

  private debeMostrarSplash(): boolean {
    try {
      if (sessionStorage.getItem(SPLASH_KEY)) return false;
      sessionStorage.setItem(SPLASH_KEY, '1');
      return true;
    } catch {
      return false;
    }
  }
}
