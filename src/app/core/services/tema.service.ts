import { Injectable, signal } from '@angular/core';

const THEME_KEY = 'ugartino-theme';

export interface TemaInstitucional {
  colorPrincipal: string;
  colorSecundario: string;
  /** Logotipo personalizado como data URL; vacío usa el escudo por defecto. */
  logo: string;
}

export const TEMA_POR_DEFECTO: TemaInstitucional = {
  colorPrincipal: '#9b1b1f',
  colorSecundario: '#fdc425',
  logo: ''
};

export const LOGO_POR_DEFECTO = 'images/escudo-alfonso-ugarte.jpg';
const HEX = /^#[0-9a-f]{6}$/i;

/**
 * Identidad visual aplicada como variables CSS. La fuente de verdad es el backend (tabla institucion);
 * en el navegador solo se guarda una copia para pintar la primera pantalla sin parpadeo.
 */
@Injectable({ providedIn: 'root' })
export class TemaService {
  readonly tema = signal<TemaInstitucional>(this.leer());

  constructor() {
    this.aplicar(this.tema());
  }

  get logo(): string { return this.tema().logo || LOGO_POR_DEFECTO; }

  esColorValido(valor: string): boolean { return HEX.test(valor.trim()); }

  /** Aplica y recuerda la identidad recibida del backend. */
  establecer(tema: TemaInstitucional): void {
    const nuevo = {
      colorPrincipal: HEX.test(tema.colorPrincipal) ? tema.colorPrincipal.toLowerCase() : TEMA_POR_DEFECTO.colorPrincipal,
      colorSecundario: HEX.test(tema.colorSecundario) ? tema.colorSecundario.toLowerCase() : TEMA_POR_DEFECTO.colorSecundario,
      logo: tema.logo ?? ''
    };
    try {
      localStorage.setItem(THEME_KEY, JSON.stringify(nuevo));
    } catch { /* el logotipo puede no caber; se usará el del servidor en la próxima carga */ }
    this.tema.set(nuevo);
    this.aplicar(nuevo);
  }

  /** Vista previa sin guardar (por ejemplo, mientras se elige un color). */
  aplicar(tema: Pick<TemaInstitucional, 'colorPrincipal' | 'colorSecundario'>): void {
    if (typeof document === 'undefined') return;
    const raiz = document.documentElement.style;
    if (HEX.test(tema.colorPrincipal)) raiz.setProperty('--color-primary', tema.colorPrincipal);
    if (HEX.test(tema.colorSecundario)) raiz.setProperty('--color-gold', tema.colorSecundario);
    document.querySelector('meta[name="theme-color"]')?.setAttribute('content', tema.colorPrincipal);
  }

  private leer(): TemaInstitucional {
    try {
      const guardado = localStorage.getItem(THEME_KEY);
      if (guardado) {
        const tema = { ...TEMA_POR_DEFECTO, ...JSON.parse(guardado) as Partial<TemaInstitucional> };
        if (HEX.test(tema.colorPrincipal) && HEX.test(tema.colorSecundario)) return tema;
      }
    } catch { /* se usa el tema por defecto */ }
    return { ...TEMA_POR_DEFECTO };
  }
}
