import { Directive, ElementRef, effect, inject, input } from '@angular/core';

/**
 * Anima un número desde su valor anterior hasta el nuevo (efecto "contador").
 * Uso: <span [appContador]="total"></span>
 */
@Directive({ selector: '[appContador]', standalone: true })
export class ContadorDirective {
  readonly appContador = input.required<number>();
  readonly duracion = input(900);

  private readonly el = inject<ElementRef<HTMLElement>>(ElementRef);
  private actual = 0;
  private frame = 0;

  constructor() {
    effect(() => this.animar(Number(this.appContador()) || 0));
  }

  private animar(destino: number): void {
    cancelAnimationFrame(this.frame);
    const reducido = typeof matchMedia === 'function' && matchMedia('(prefers-reduced-motion: reduce)').matches;
    const desde = this.actual;
    const inicio = performance.now();
    const duracion = reducido ? 0 : this.duracion();

    const paso = (ahora: number) => {
      const t = duracion === 0 ? 1 : Math.min(1, (ahora - inicio) / duracion);
      this.actual = desde + (destino - desde) * (1 - Math.pow(1 - t, 3));
      this.el.nativeElement.textContent = String(Math.round(this.actual));
      if (t < 1) this.frame = requestAnimationFrame(paso);
      else this.actual = destino;
    };
    this.frame = requestAnimationFrame(paso);
  }
}
