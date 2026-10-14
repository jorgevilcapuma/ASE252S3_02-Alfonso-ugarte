import { Component, DestroyRef, OnInit, computed, inject, input, signal } from '@angular/core';
import { AsistenciaService } from '../../../core/services/asistencia.service';
import { TemaService } from '../../../core/services/tema.service';
import { IconoComponent } from '../icono/icono.component';

/** Duración de la animación de ingreso; el login espera este tiempo antes de navegar. */
export const DURACION_AUTENTICACION = 1500;

/**
 * Pantalla completa de carga.
 * - `inicio`: presentación al abrir el portal (fondo institucional).
 * - `sesion`: "Autenticando sesión" tras enviar el formulario de acceso, con pasos y progreso.
 */
@Component({
  selector: 'app-pantalla-carga',
  standalone: true,
  imports: [IconoComponent],
  template: `
    <div class="carga" [class.is-inicio]="variante() === 'inicio'" [class.is-saliendo]="saliendo()" role="status" aria-live="polite">
      @if (variante() === 'inicio') {
        <div class="crest"><img [src]="tema.logo" alt=""></div>
        <h1>Portal Ugartino Digital</h1>
        <p class="tag">Control escolar · {{ data.nombreInstitucion }}</p>
        <span class="ring" aria-hidden="true"></span>
        <small>Iniciando sistema…</small>
      } @else {
        <div class="fondo" aria-hidden="true"><i></i><i></i><i></i></div>
        <section class="tarjeta">
          <div class="halo" aria-hidden="true">
            <span class="anillo anillo-oro"></span>
            <span class="anillo anillo-granate"></span>
            <span class="escudo"><img [src]="tema.logo" alt=""></span>
          </div>

          <span class="etiqueta">{{ perfil() ? 'Acceso ' + perfil() : 'Acceso institucional' }}</span>
          <h1>Autenticando sesión</h1>
          <p>
            @if (usuario()) { Hola, <strong>{{ usuario() }}</strong>. }
            Verificando credenciales institucionales de {{ data.nombreInstitucion }}.
          </p>

          <ol class="pasos">
            @for (texto of pasos(); track texto; let i = $index) {
              <li [class.is-hecho]="paso() > i" [class.is-actual]="paso() === i">
                <span class="marca">
                  @if (paso() > i) { <app-icono nombre="check" [tamano]="14" /> }
                  @else if (paso() === i) { <i class="mini-spinner"></i> }
                </span>
                {{ texto }}
              </li>
            }
          </ol>

          <div class="progreso">
            <div class="barra"><span [style.width.%]="progreso()"></span></div>
            <b>{{ progreso() }}%</b>
          </div>

          <small class="seguro"><app-icono nombre="candado" [tamano]="14" /> Conexión segura · Portal Ugartino Digital</small>
        </section>
      }
    </div>
  `,
  styles: [`
    .carga { position: fixed; z-index: 500; inset: 0; display: grid; place-content: center; justify-items: center; gap: 14px; overflow: hidden; padding: 24px; color: var(--color-ink); background: var(--color-canvas); text-align: center; animation: entrar .25s ease both; }
    .carga.is-saliendo { animation: salir .35s ease forwards; }

    /* ---- Presentación inicial ---- */
    .carga.is-inicio { color: #fff; background: radial-gradient(circle at 50% 35%, var(--color-primary) 0%, var(--color-primary-strong) 75%); }
    .crest { display: grid; width: 132px; height: 132px; place-items: center; margin-bottom: 8px; border: 4px solid var(--color-gold); border-radius: 28px; background: #fff; box-shadow: 0 20px 50px rgba(0, 0, 0, .3); animation: latido 2.2s ease-in-out infinite; }
    .crest img { width: 100px; height: 100px; object-fit: contain; }
    h1 { margin: 0; color: inherit; font-size: clamp(24px, 4vw, 32px); font-weight: 800; letter-spacing: -.02em; }
    .tag { margin: 0; color: var(--color-gold); font-size: 12px; font-weight: 700; letter-spacing: .14em; text-transform: uppercase; }
    small { margin-top: 18px; color: rgba(255, 255, 255, .65); font-size: 11px; font-weight: 700; letter-spacing: .14em; text-transform: uppercase; }
    .ring { width: 36px; height: 36px; margin-top: 18px; border: 3px solid rgba(255, 255, 255, .25); border-top-color: var(--color-gold); border-radius: 50%; animation: girar .8s linear infinite; }

    /* ---- Autenticando sesión ---- */
    .fondo { position: absolute; inset: 0; background-image: radial-gradient(color-mix(in srgb, var(--color-primary) 10%, transparent) 1.2px, transparent 1.2px); background-size: 24px 24px; }
    .fondo i { position: absolute; border-radius: 50%; filter: blur(70px); opacity: .5; animation: flotar 6s ease-in-out infinite; }
    .fondo i:nth-child(1) { top: -120px; left: -100px; width: 420px; height: 420px; background: color-mix(in srgb, var(--color-primary) 30%, transparent); }
    .fondo i:nth-child(2) { right: -140px; bottom: -160px; width: 480px; height: 480px; background: color-mix(in srgb, var(--color-gold) 45%, transparent); animation-delay: -3s; }
    .fondo i:nth-child(3) { top: 40%; left: 60%; width: 220px; height: 220px; background: color-mix(in srgb, var(--color-primary) 18%, transparent); animation-delay: -1.5s; }

    .tarjeta { position: relative; display: grid; justify-items: center; width: min(100%, 440px); padding: 40px 36px 26px; overflow: hidden; border: 1px solid var(--color-primary-line); border-radius: 20px; background: rgba(255, 255, 255, .92); box-shadow: 0 30px 70px rgba(20, 27, 45, .14); backdrop-filter: blur(8px); animation: subir .45s cubic-bezier(.2, .8, .2, 1) both; }
    .tarjeta::before { position: absolute; top: 0; left: 0; right: 0; height: 5px; background: linear-gradient(90deg, var(--color-primary), var(--color-gold)); content: ''; }

    .halo { position: relative; display: grid; width: 116px; height: 116px; place-items: center; margin-bottom: 18px; }
    .anillo { position: absolute; border-radius: 50%; }
    .anillo-oro { inset: 0; border: 3px solid transparent; border-top-color: var(--color-gold); border-right-color: var(--color-gold); animation: girar 1.4s linear infinite; }
    .anillo-granate { inset: 10px; border: 3px solid var(--color-primary-soft); border-bottom-color: var(--color-primary); animation: girar 1s linear infinite reverse; }
    .escudo { display: grid; width: 74px; height: 74px; place-items: center; border-radius: 50%; background: #fff; box-shadow: 0 0 0 0 color-mix(in srgb, var(--color-primary) 35%, transparent); animation: brillo 1.6s ease-out infinite; }
    .escudo img { width: 50px; height: 50px; object-fit: contain; }

    .etiqueta { margin-bottom: 6px; padding: 4px 12px; border-radius: 999px; color: var(--color-primary-strong); background: var(--color-primary-soft); font-size: 11.5px; font-weight: 700; letter-spacing: .1em; text-transform: uppercase; }
    .tarjeta h1 { font-size: 28px; }
    .tarjeta p { max-width: 34ch; margin: 8px 0 0; color: var(--color-muted); font-size: 14.5px; line-height: 1.5; }
    .tarjeta p strong { color: var(--color-ink); }

    .pasos { display: grid; gap: 10px; width: 100%; margin: 24px 0 0; padding: 16px 18px; border-radius: 14px; background: var(--color-surface-2); list-style: none; text-align: left; }
    .pasos li { display: flex; align-items: center; gap: 12px; color: #a1a8b6; font-size: 14px; font-weight: 600; transition: color .25s ease; }
    .pasos li.is-actual { color: var(--color-ink); }
    .pasos li.is-hecho { color: var(--color-text); }
    .marca { display: grid; width: 22px; height: 22px; flex: 0 0 22px; place-items: center; border: 2px solid #d5d9e2; border-radius: 50%; transition: background .25s ease, border-color .25s ease; }
    .is-actual .marca { border-color: var(--color-primary-line); }
    .is-hecho .marca { border-color: transparent; color: #fff; background: var(--color-success-solid); animation: pop .35s cubic-bezier(.3, 1.5, .5, 1) both; }
    .mini-spinner { width: 12px; height: 12px; border: 2px solid var(--color-primary-soft); border-top-color: var(--color-primary); border-radius: 50%; animation: girar .7s linear infinite; }

    .progreso { display: flex; align-items: center; gap: 12px; width: 100%; margin-top: 20px; }
    .barra { flex: 1; height: 8px; overflow: hidden; border-radius: 8px; background: var(--color-line-soft); }
    .barra span { position: relative; display: block; height: 100%; overflow: hidden; border-radius: inherit; background: linear-gradient(90deg, var(--color-primary), var(--color-gold)); }
    .barra span::after { position: absolute; inset: 0; background: linear-gradient(90deg, transparent, rgba(255, 255, 255, .55), transparent); content: ''; animation: barrido 1s linear infinite; }
    .progreso b { min-width: 42px; color: var(--color-primary-strong); font-size: 14px; font-variant-numeric: tabular-nums; text-align: right; }
    .seguro { display: inline-flex; align-items: center; gap: 6px; margin-top: 20px; color: var(--color-muted); font-size: 12px; font-weight: 600; letter-spacing: 0; text-transform: none; }
    .seguro app-icono { color: var(--color-success); }

    @keyframes girar { to { transform: rotate(360deg); } }
    @keyframes latido { 0%, 100% { transform: scale(1); } 50% { transform: scale(1.04); } }
    @keyframes brillo { 0% { box-shadow: 0 0 0 0 color-mix(in srgb, var(--color-primary) 35%, transparent); } 100% { box-shadow: 0 0 0 18px transparent; } }
    @keyframes barrido { from { transform: translateX(-100%); } to { transform: translateX(100%); } }
    @keyframes flotar { 0%, 100% { transform: translate(0, 0); } 50% { transform: translate(20px, -24px); } }
    @keyframes subir { from { opacity: 0; transform: translateY(16px) scale(.97); } }
    @keyframes pop { 0% { transform: scale(.5); } 100% { transform: scale(1); } }
    @keyframes entrar { from { opacity: 0; } }
    @keyframes salir { to { opacity: 0; visibility: hidden; } }
    @media (max-width: 480px) { .tarjeta { padding: 32px 20px 22px; } .tarjeta h1 { font-size: 23px; } }
  `]
})
export class PantallaCargaComponent implements OnInit {
  readonly variante = input<'inicio' | 'sesion'>('inicio');
  readonly saliendo = input(false);
  /** Nombre y perfil de quien ingresa (solo variante `sesion`). */
  readonly usuario = input('');
  readonly perfil = input('');

  readonly data = inject(AsistenciaService);
  readonly tema = inject(TemaService);
  private readonly destroyRef = inject(DestroyRef);

  readonly paso = signal(0);
  readonly progreso = signal(0);
  readonly pasos = computed(() => [
    'Verificando credenciales',
    `Cargando perfil${this.perfil() ? ' de ' + this.perfil().toLowerCase() : ''}`,
    'Preparando tu panel'
  ]);

  ngOnInit(): void {
    if (this.variante() !== 'sesion') return;
    const inicio = performance.now();
    const total = DURACION_AUTENTICACION - 150;
    let cuadro = 0;
    const animar = (ahora: number) => {
      const avance = Math.min(1, (ahora - inicio) / total);
      // Curva suave: avanza rápido al inicio y frena al final.
      this.progreso.set(Math.round((1 - Math.pow(1 - avance, 2.2)) * 100));
      this.paso.set(Math.min(3, Math.floor(avance * 3.2)));
      if (avance < 1) cuadro = requestAnimationFrame(animar);
    };
    cuadro = requestAnimationFrame(animar);
    this.destroyRef.onDestroy(() => cancelAnimationFrame(cuadro));
  }
}
