import { CommonModule } from '@angular/common';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { NivelEducativo } from '../../core/models/asistencia.model';
import { AsistenciaService, FaseProyecto } from '../../core/services/asistencia.service';
import { IconoComponent, NombreIcono } from '../../shared/components/icono/icono.component';
import { ContadorDirective } from '../../shared/directives/contador.directive';

interface Aviso { etiqueta: string; titulo: string; texto: string; icono: NombreIcono; tono: 'rojo' | 'dorado' | 'azul'; imagen: string; }

const AVISOS: Aviso[] = [
  { etiqueta: 'Campaña de prevención', titulo: '¡Asistencia perfecta!', texto: 'La puntualidad es el primer paso hacia el éxito. Conoce las directivas de la UGEL 08 para el registro diario de asistencia.', icono: 'reloj', tono: 'rojo',
    imagen: 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTOHQUat400DHFxCogP2JL7YPQzv3C5c20mTrd5wpDB3Q&s=10' },
  { etiqueta: 'Comunidad educativa', titulo: 'Cada día cuenta', texto: 'Las familias pueden registrar justificaciones en línea desde el portal del alumno, con sustento adjunto.', icono: 'alumnos', tono: 'dorado',
    imagen: 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRq77_G7i-Qjb_zfZrgApei_wCQA3WV3X1idLY65vxeCw&s=10' },
  { etiqueta: 'Alertas tempranas', titulo: 'Acompañamiento oportuno', texto: 'Tres faltas acumuladas activan una alerta crítica. Coordina con tutoría y comunica a la familia a tiempo.', icono: 'alerta', tono: 'azul',
    imagen: 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQWjNxMbmmZy4u2XW6dJYfQDqfLf0TydwJIyvj0K0ndew&s' }
];

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, IconoComponent, ContadorDirective],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent {
  readonly data = inject(AsistenciaService);
  readonly avisos = AVISOS;
  readonly actual = signal(0);
  private pausado = false;

  constructor() {
    // Registro de hoy actualizado desde el backend (otros docentes pueden estar registrando).
    void this.data.asegurarGestion().then(() => this.data.cargarAsistencia(this.data.hoy, this.data.hoy)).catch(() => undefined);
    const temporizador = setInterval(() => { if (!this.pausado) this.siguiente(); }, 6500);
    inject(DestroyRef).onDestroy(() => clearInterval(temporizador));
  }

  /** Fase en curso del proyecto (tabla fase_proyecto). */
  get faseActual() {
    const fases = this.data.fases;
    const indice = Math.max(0, fases.findIndex(fase => fase.estado === 'En curso'));
    const completadas = fases.filter(fase => fase.estado === 'Completada').length;
    return {
      numero: String(indice + 1).padStart(2, '0'),
      total: String(fases.length || 8).padStart(2, '0'),
      fase: fases[indice] as FaseProyecto | undefined,
      avance: fases.length ? Math.round(completadas / fases.length * 100) : 0
    };
  }

  get totalGrados(): number { return Object.values(this.data.gradosPorNivel).reduce((total, grados) => total + grados.length, 0); }

  readonly bloquesNivel: { nivel: NivelEducativo; icono: NombreIcono; unidad: string }[] = [
    { nivel: 'Inicial', icono: 'sol', unidad: 'Aula' },
    { nivel: 'Primaria', icono: 'libro', unidad: 'Grado' },
    { nivel: 'Secundaria', icono: 'birrete', unidad: 'Año' }
  ];

  registroGrado(nivel: NivelEducativo, grado: string): number {
    const estudiantes = this.data.estudiantes.filter(item => item.nivel === nivel && item.grado === grado);
    if (estudiantes.length === 0) return 0;
    return Math.round(estudiantes.filter(item => this.data.obtenerEstado(item.id, this.data.hoy) !== 'pendiente').length / estudiantes.length * 100);
  }

  etiquetaCorta(nivel: NivelEducativo, grado: string): string {
    return nivel === 'Inicial' ? grado.replace(' años', '') : `${grado}°`;
  }

  siguiente(): void { this.actual.update(indice => (indice + 1) % this.avisos.length); }
  anterior(): void { this.actual.update(indice => (indice - 1 + this.avisos.length) % this.avisos.length); }
  pausar(valor: boolean): void { this.pausado = valor; }
}
