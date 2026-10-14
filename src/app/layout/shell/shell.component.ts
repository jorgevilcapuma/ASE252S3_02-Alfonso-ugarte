import { DatePipe } from '@angular/common';
import { Component, DestroyRef, ElementRef, HostListener, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NavigationCancel, NavigationEnd, NavigationError, NavigationStart, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { ACCION_RAPIDA, ETIQUETA_PERFIL, EnlaceNavegacion, NAVEGACION } from '../../core/config/navegacion';
import { AsistenciaService } from '../../core/services/asistencia.service';
import { AuthService } from '../../core/services/auth.service';
import { ModoDatosService } from '../../core/services/modo-datos.service';
import { NotificacionService } from '../../core/services/notificacion.service';
import { TemaService } from '../../core/services/tema.service';
import { AlumnoService } from '../../features/alumno/alumno.service';
import { DocenteService } from '../../features/docente/docente.service';
import { IconoComponent } from '../../shared/components/icono/icono.component';

type InfoPie = 'soporte' | 'privacidad' | 'manual';

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [DatePipe, RouterLink, RouterLinkActive, RouterOutlet, IconoComponent],
  templateUrl: './shell.component.html',
  styleUrl: './shell.component.css'
})
export class ShellComponent {
  readonly auth = inject(AuthService);
  readonly data = inject(AsistenciaService);
  readonly tema = inject(TemaService);
  readonly modo = inject(ModoDatosService);
  private readonly router = inject(Router);
  private readonly notificaciones = inject(NotificacionService);
  private readonly host = inject(ElementRef<HTMLElement>);

  /** Estado visual: menú móvil, menú de usuario, barra de carga, reloj y ventana informativa del pie. */
  readonly menuAbierto = signal(false);
  readonly menuUsuario = signal(false);
  readonly cargando = signal(false);
  readonly ahora = signal(new Date());
  readonly infoPie = signal<InfoPie | null>(null);
  readonly anio = new Date().getFullYear();

  constructor() {
    // Datos del perfil desde el backend: el menú se dibuja de inmediato y las pantallas se completan al llegar.
    if (this.perfil === 'alumno') {
      void this.data.asegurarBase();
      void inject(AlumnoService).asegurar();
    } else {
      void this.data.asegurarGestion();
      if (this.perfil === 'docente') void inject(DocenteService).asegurar();
    }

    const destroyRef = inject(DestroyRef);
    const reloj = setInterval(() => this.ahora.set(new Date()), 30_000);
    destroyRef.onDestroy(() => clearInterval(reloj));

    this.router.events.pipe(takeUntilDestroyed()).subscribe(evento => {
      if (evento instanceof NavigationStart) this.cargando.set(true);
      if (evento instanceof NavigationEnd || evento instanceof NavigationCancel || evento instanceof NavigationError) {
        setTimeout(() => this.cargando.set(false), 250);
        this.menuAbierto.set(false);
        this.menuUsuario.set(false);
      }
    });
  }

  private get perfil() { return this.auth.sesion?.perfil ?? 'administrador'; }
  private get enlaces(): EnlaceNavegacion[] { return NAVEGACION.filter(enlace => enlace.perfil === this.perfil); }

  get grupos(): { nombre: string; enlaces: EnlaceNavegacion[] }[] {
    const visibles = this.enlaces.filter(enlace => !enlace.pie);
    return ['Principal', 'Gestión', 'Comunidad', 'Sistema']
      .map(nombre => ({ nombre, enlaces: visibles.filter(enlace => enlace.grupo === nombre) }))
      .filter(grupo => grupo.enlaces.length > 0);
  }

  get enlacesPie(): EnlaceNavegacion[] { return this.enlaces.filter(enlace => enlace.pie); }
  get accionRapida() { return ACCION_RAPIDA[this.perfil]; }

  get nombreUsuario(): string { return this.auth.sesion?.nombre || 'Usuario'; }
  get primerNombre(): string { return this.nombreUsuario.split(' ')[0]; }
  get perfilLabel(): string { return ETIQUETA_PERFIL[this.perfil]; }
  get iniciales(): string { return this.nombreUsuario.split(' ').filter(Boolean).slice(0, 2).map(parte => parte[0]).join('').toUpperCase(); }
  get muestraAlertas(): boolean { return this.perfil !== 'alumno'; }
  get rutaAlertas(): string { return this.perfil === 'docente' ? '/docente/alertas' : '/administrador/alertas'; }

  /** "I.E. 20191 Alfonso Ugarte" → código en una línea y nombre en la siguiente. */
  get marca(): { codigo: string; nombre: string } {
    const nombre = this.data.nombreInstitucion.trim();
    const coincidencia = nombre.match(/^(I\.?\s?E\.?\s*N?°?\s*\d+)\s*(.*)$/i);
    return coincidencia ? { codigo: coincidencia[1], nombre: coincidencia[2] || 'Institución educativa' } : { codigo: nombre, nombre: 'Institución educativa' };
  }

  get saludo(): string {
    const hora = this.ahora().getHours();
    return hora < 12 ? 'Buenos días' : hora < 19 ? 'Buenas tardes' : 'Buenas noches';
  }

  get tituloPagina(): string {
    const ruta = this.router.url.split(/[?#]/)[0];
    return NAVEGACION.find(enlace => enlace.ruta === ruta)?.etiqueta ?? 'Portal';
  }

  alternarMenu(): void { this.menuAbierto.update(abierto => !abierto); }
  alternarMenuUsuario(): void { this.menuUsuario.update(abierto => !abierto); }

  @HostListener('document:keydown.escape')
  cerrarMenu(): void {
    this.menuAbierto.set(false);
    this.menuUsuario.set(false);
    this.infoPie.set(null);
  }

  /** Cierra el menú de usuario al hacer clic fuera de él. */
  @HostListener('document:click', ['$event'])
  clicFuera(evento: MouseEvent): void {
    if (!this.menuUsuario()) return;
    const menu = (this.host.nativeElement as HTMLElement).querySelector('.user-menu');
    if (menu && !menu.contains(evento.target as Node)) this.menuUsuario.set(false);
  }

  salir(): void {
    const nombre = this.primerNombre;
    this.auth.signOut();
    void this.router.navigateByUrl('/login');
    this.notificaciones.info('Sesión cerrada', `Hasta pronto, ${nombre}.`);
  }
}
