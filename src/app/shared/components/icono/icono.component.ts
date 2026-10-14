import { Component, computed, inject, input } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

export type NombreIcono =
  | 'panel' | 'asistencia' | 'usuarios' | 'matricula' | 'justificacion' | 'alerta' | 'fases' | 'ajustes'
  | 'cursos' | 'horario' | 'mensaje' | 'perfil' | 'inicio' | 'salir' | 'campana' | 'flecha' | 'check'
  | 'descargar' | 'mas' | 'buscar' | 'info' | 'alumnos' | 'reloj' | 'escuela' | 'reporte'
  | 'correo' | 'candado' | 'ojo' | 'ojo-cerrado' | 'credencial' | 'entrar' | 'atras' | 'derecha' | 'izquierda'
  | 'megafono' | 'guardar' | 'paleta' | 'edificio' | 'estructura' | 'enviar' | 'documento' | 'calendario'
  | 'cerrar' | 'secciones' | 'matriz' | 'birrete' | 'libro' | 'sol' | 'tendencia' | 'subir'
  | 'editar' | 'papelera' | 'traslado';

/* Íconos de trazo (24×24), un solo lugar para mantener coherencia visual. */
const TRAZOS: Record<NombreIcono, string> = {
  panel: '<rect x="3" y="3" width="7" height="7" rx="1.5"/><rect x="14" y="3" width="7" height="7" rx="1.5"/><rect x="3" y="14" width="7" height="7" rx="1.5"/><rect x="14" y="14" width="7" height="7" rx="1.5"/>',
  inicio: '<path d="M3 11.5 12 4l9 7.5"/><path d="M5 10v10h14V10"/>',
  asistencia: '<rect x="4" y="3" width="16" height="18" rx="2.5"/><path d="m8.5 12 2.3 2.3L15.5 9.5"/>',
  usuarios: '<circle cx="9" cy="8" r="3.5"/><path d="M2.5 20c.6-3.6 3.2-5.5 6.5-5.5s5.9 1.9 6.5 5.5"/><path d="M16 4.6a3.5 3.5 0 0 1 0 6.8M18.5 14.8c1.8.7 2.8 2.3 3 5.2"/>',
  alumnos: '<circle cx="12" cy="8" r="3.5"/><path d="M5 20c.7-3.7 3.4-5.6 7-5.6s6.3 1.9 7 5.6"/>',
  perfil: '<circle cx="12" cy="8.5" r="3.6"/><path d="M4.5 20.5c.8-3.8 3.6-6 7.5-6s6.7 2.2 7.5 6"/>',
  matricula: '<path d="M4 5.5A2.5 2.5 0 0 1 6.5 3H20v15H6.5A2.5 2.5 0 0 0 4 20.5z"/><path d="M4 20.5A2.5 2.5 0 0 0 6.5 23H20v-5"/><path d="M9 8h7"/>',
  justificacion: '<path d="M14 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8z"/><path d="M14 3v5h5M9 13h6M9 17h4"/>',
  alerta: '<path d="M12 3.5 2.8 19.5h18.4z"/><path d="M12 10v4.5M12 17.4v.1"/>',
  fases: '<circle cx="12" cy="12" r="9"/><path d="M12 7v5l3.2 2"/>',
  reloj: '<circle cx="12" cy="12" r="9"/><path d="M12 7v5l3.2 2"/>',
  ajustes: '<circle cx="12" cy="12" r="3.2"/><path d="M19.4 14.5a1.6 1.6 0 0 0 .3 1.8l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.6 1.6 0 0 0-1.8-.3 1.6 1.6 0 0 0-1 1.5V21a2 2 0 1 1-4 0v-.1a1.6 1.6 0 0 0-1-1.5 1.6 1.6 0 0 0-1.8.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.6 1.6 0 0 0 .3-1.8 1.6 1.6 0 0 0-1.5-1H3a2 2 0 1 1 0-4h.1a1.6 1.6 0 0 0 1.5-1 1.6 1.6 0 0 0-.3-1.8l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.6 1.6 0 0 0 1.8.3h0a1.6 1.6 0 0 0 1-1.5V3a2 2 0 1 1 4 0v.1a1.6 1.6 0 0 0 1 1.5 1.6 1.6 0 0 0 1.8-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.6 1.6 0 0 0-.3 1.8v0a1.6 1.6 0 0 0 1.5 1H21a2 2 0 1 1 0 4h-.1a1.6 1.6 0 0 0-1.5 1z"/>',
  cursos: '<path d="M3 7.5 12 3l9 4.5-9 4.5z"/><path d="M7 10v5.5c0 1.2 2.2 2.5 5 2.5s5-1.3 5-2.5V10"/>',
  horario: '<rect x="3.5" y="5" width="17" height="16" rx="2.5"/><path d="M3.5 10h17M8 3v4M16 3v4"/>',
  mensaje: '<path d="M4 5h16a1 1 0 0 1 1 1v10a1 1 0 0 1-1 1h-8l-5 4v-4H4a1 1 0 0 1-1-1V6a1 1 0 0 1 1-1z"/>',
  salir: '<path d="M14 4h4a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2h-4"/><path d="M10 16.5 14.5 12 10 7.5M14.5 12H3.5"/>',
  campana: '<path d="M6 16.5V11a6 6 0 1 1 12 0v5.5l1.5 2h-15z"/><path d="M10 21a2.2 2.2 0 0 0 4 0"/>',
  flecha: '<path d="M7 17 17 7M9 7h8v8"/>',
  check: '<path d="m5 12.5 4.5 4.5L19 7.5"/>',
  descargar: '<path d="M12 4v11M7.5 10.5 12 15l4.5-4.5M5 19.5h14"/>',
  mas: '<path d="M12 5v14M5 12h14"/>',
  buscar: '<circle cx="11" cy="11" r="6.5"/><path d="m20 20-4.3-4.3"/>',
  info: '<circle cx="12" cy="12" r="9"/><path d="M12 11v5.5M12 7.6v.1"/>',
  escuela: '<path d="M3 10 12 4l9 6v1H3z"/><path d="M5 11v8M9.5 11v8M14.5 11v8M19 11v8M3 20h18"/>',
  reporte: '<rect x="3.5" y="3.5" width="17" height="17" rx="2.5"/><path d="M8 16.5v-3M12 16.5v-8M16 16.5v-5"/>',
  correo: '<rect x="3" y="5" width="18" height="14" rx="2"/><path d="m3.5 6.5 8.5 6.5 8.5-6.5"/>',
  candado: '<rect x="4.5" y="10.5" width="15" height="10" rx="2"/><path d="M8 10.5V7.5a4 4 0 0 1 8 0v3M12 14.5v2.5"/>',
  ojo: '<path d="M2.5 12S6 5.5 12 5.5 21.5 12 21.5 12 18 18.5 12 18.5 2.5 12 2.5 12z"/><circle cx="12" cy="12" r="3"/>',
  'ojo-cerrado': '<path d="M3 3l18 18M10.6 5.6A9.7 9.7 0 0 1 12 5.5C18 5.5 21.5 12 21.5 12a17 17 0 0 1-3.1 3.9M6.2 6.6C3.9 8.2 2.5 12 2.5 12S6 18.5 12 18.5c1.6 0 3-.4 4.2-1.1M9.9 9.9a3 3 0 0 0 4.2 4.2"/>',
  credencial: '<rect x="3" y="5.5" width="18" height="14" rx="2"/><circle cx="9" cy="11.5" r="2.2"/><path d="M5.8 16.5c.5-1.6 1.7-2.4 3.2-2.4s2.7.8 3.2 2.4M14.5 10.5H18M14.5 13.5H17M10 3.5h4v3h-4z"/>',
  entrar: '<path d="M14 4h4a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2h-4"/><path d="M9.5 16.5 14 12 9.5 7.5M14 12H3.5"/>',
  atras: '<path d="M19.5 12h-15M10.5 18l-6-6 6-6"/>',
  derecha: '<path d="m9.5 6 6 6-6 6"/>',
  izquierda: '<path d="m14.5 6-6 6 6 6"/>',
  megafono: '<path d="M3.5 10v4a1 1 0 0 0 1 1H7l7 4.5v-15L7 9H4.5a1 1 0 0 0-1 1z"/><path d="M7 15l1.5 5h2.5L10 15.6M17.5 9a4 4 0 0 1 0 6M20 7a7 7 0 0 1 0 10"/>',
  guardar: '<path d="M5 3.5h11.5L20.5 7.5V19a1.5 1.5 0 0 1-1.5 1.5H5A1.5 1.5 0 0 1 3.5 19V5A1.5 1.5 0 0 1 5 3.5z"/><path d="M7.5 3.5v5h8v-5M7.5 20.5v-6.5h9v6.5"/>',
  paleta: '<path d="M12 3a9 9 0 1 0 0 18c1.1 0 1.8-.8 1.8-1.7 0-.5-.2-.9-.5-1.2-.3-.3-.5-.7-.5-1.2 0-.9.8-1.7 1.7-1.7h2.1A4.4 4.4 0 0 0 21 10.9C21 6.5 17 3 12 3z"/><circle cx="7.5" cy="11" r="1.1"/><circle cx="10" cy="7" r="1.1"/><circle cx="14.5" cy="7" r="1.1"/>',
  edificio: '<path d="M4 21V5.5A1.5 1.5 0 0 1 5.5 4h7A1.5 1.5 0 0 1 14 5.5V21M14 10h4.5A1.5 1.5 0 0 1 20 11.5V21M2.5 21h19"/><path d="M7.5 8h3M7.5 12h3M7.5 16h3M16.5 14h1M16.5 17.5h1"/>',
  estructura: '<rect x="3" y="3.5" width="6" height="5" rx="1"/><rect x="15" y="9.5" width="6" height="5" rx="1"/><rect x="15" y="16" width="6" height="5" rx="1"/><path d="M6 8.5v10h9M6 12h9"/>',
  enviar: '<path d="M21 3 3.5 10.5l7 2.5 2.5 7z"/><path d="M21 3 10.5 13"/>',
  documento: '<path d="M14 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8z"/><path d="M14 3v5h5"/><path d="M8.5 15.5h1.2a1.3 1.3 0 0 0 0-2.6H8.5V18M13 12.9V18h1a2 2 0 0 0 2-2v-1.1a2 2 0 0 0-2-2z"/>',
  calendario: '<rect x="3.5" y="5" width="17" height="16" rx="2.5"/><path d="M3.5 10h17M8 3v4M16 3v4M8 14h2M14 14h2M8 17.5h2"/>',
  cerrar: '<path d="M6 6l12 12M18 6 6 18"/>',
  secciones: '<path d="M8.5 6.5h12M8.5 12h12M8.5 17.5h12"/><circle cx="4.5" cy="6.5" r="1"/><circle cx="4.5" cy="12" r="1"/><circle cx="4.5" cy="17.5" r="1"/>',
  matriz: '<rect x="3.5" y="3.5" width="17" height="17" rx="2"/><path d="M3.5 9h17M9.5 9v11.5M15 9v11.5"/>',
  birrete: '<path d="M2.5 9 12 4.5 21.5 9 12 13.5z"/><path d="M6.5 11v5c0 1.4 2.5 3 5.5 3s5.5-1.6 5.5-3v-5M21.5 9v5"/>',
  libro: '<path d="M12 6.5C10 5 7.5 4.5 3.5 4.5v14c4 0 6.5.5 8.5 2 2-1.5 4.5-2 8.5-2v-14c-4 0-6.5.5-8.5 2z"/><path d="M12 6.5v14"/>',
  sol: '<circle cx="12" cy="12" r="4"/><path d="M12 2.5v2M12 19.5v2M2.5 12h2M19.5 12h2M5.3 5.3l1.4 1.4M17.3 17.3l1.4 1.4M5.3 18.7l1.4-1.4M17.3 6.7l1.4-1.4"/>',
  tendencia: '<path d="m3 17 6-6 4 4 8-8"/><path d="M15 7h6v6"/>',
  subir: '<path d="M12 16V5M7.5 9.5 12 5l4.5 4.5M5 19.5h14"/>',
  editar: '<path d="M4 20h4L19 9a2.8 2.8 0 0 0-4-4L4 16z"/><path d="m13.5 6.5 4 4"/>',
  papelera: '<path d="M4 7h16M9.5 7V4.5h5V7M6 7l1 13h10l1-13M10 11v5.5M14 11v5.5"/>',
  traslado: '<path d="M4 8h14M14.5 4.5 18 8l-3.5 3.5M20 16H6M9.5 12.5 6 16l3.5 3.5"/>'
};

@Component({
  selector: 'app-icono',
  standalone: true,
  template: `<svg [attr.width]="tamano()" [attr.height]="tamano()" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" [innerHTML]="trazo()"></svg>`,
  styles: [`:host { display: inline-flex; line-height: 0; } svg { display: block; }`]
})
export class IconoComponent {
  readonly nombre = input.required<NombreIcono>();
  readonly tamano = input(20);
  private readonly sanitizer = inject(DomSanitizer);
  /* TRAZOS es una constante del proyecto (nunca entrada de usuario), por eso es seguro confiar en ella. */
  protected readonly trazo = computed<SafeHtml>(() => this.sanitizer.bypassSecurityTrustHtml(TRAZOS[this.nombre()]));
}
