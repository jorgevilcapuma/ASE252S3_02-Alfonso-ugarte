import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { IconoComponent } from '../../shared/components/icono/icono.component';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AsistenciaService } from '../../core/services/asistencia.service';
import { EstadoJustificacion, Justificacion } from '../../core/models/asistencia.model';
import { ApiService } from '../../core/services/api.service';
import { NotificacionService } from '../../core/services/notificacion.service';

type FiltroEstado = EstadoJustificacion | 'Todas';

@Component({
  selector: 'app-justificaciones',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, IconoComponent],
  templateUrl: './justificaciones.component.html',
  styleUrl: './justificaciones.component.css'
})
export class JustificacionesComponent {
  readonly data = inject(AsistenciaService);
  private readonly notificaciones = inject(NotificacionService);
  readonly filtros: { valor: FiltroEstado; etiqueta: string }[] = [
    { valor: 'Pendiente', etiqueta: 'Pendientes' },
    { valor: 'Aprobada', etiqueta: 'Aprobadas' },
    { valor: 'Observada', etiqueta: 'Rechazadas' },
    { valor: 'Todas', etiqueta: 'Todas' }
  ];
  observaciones: Record<number, string> = {};
  mensaje = '';
  busqueda = '';
  /** Se empieza por lo que requiere acción; si no hay pendientes, se muestran todas. */
  filtro: FiltroEstado = 'Pendiente';
  revisando: number | null = null;

  constructor() {
    void this.data.asegurarGestion()
      .then(() => this.data.cargarJustificaciones())
      .then(() => { if (!this.data.justificaciones.some(item => item.estado === 'Pendiente')) this.filtro = 'Todas'; })
      .catch(error => this.notificaciones.error('No se pudieron cargar las justificaciones', ApiService.mensaje(error)));
  }

  get pendientes(): number { return this.contar('Pendiente'); }

  get filtradas(): Justificacion[] {
    const texto = this.normalizar(this.busqueda);
    return this.data.justificaciones.filter(item =>
      (this.filtro === 'Todas' || item.estado === this.filtro) &&
      (!texto || this.normalizar(item.estudianteNombre).includes(texto) || this.normalizar(item.motivo).includes(texto))
    );
  }

  contar(estado: FiltroEstado): number {
    return estado === 'Todas' ? this.data.justificaciones.length : this.data.justificaciones.filter(item => item.estado === estado).length;
  }

  aula(item: Justificacion): string {
    const estudiante = this.data.estudiantes.find(e => e.id === item.estudianteId);
    const aula = estudiante ? `${estudiante.grado}${estudiante.nivel === 'Inicial' ? '' : '°'} "${estudiante.seccion}" · ${estudiante.nivel}` : '';
    return item.cursoNombre ? `${aula} · ${item.cursoNombre}` : aula;
  }

  iniciales(nombre: string): string { return nombre.split(' ').slice(0, 2).map(parte => parte[0]).join('').toUpperCase(); }

  async revisar(justificacion: Justificacion, estado: 'Aprobada' | 'Observada'): Promise<void> {
    const observacion = this.observaciones[justificacion.id]?.trim();
    if (estado === 'Observada' && (!observacion || observacion.length < 5)) {
      this.mensaje = `Escribe una observación (al menos 5 caracteres) para devolver la solicitud de ${justificacion.estudianteNombre}.`;
      return;
    }
    if (this.revisando !== null) return;
    this.revisando = justificacion.id;
    const error = await this.data.revisarJustificacion(justificacion, estado, observacion);
    this.revisando = null;
    if (error) {
      this.mensaje = error;
      return;
    }
    this.mensaje = '';
    if (estado === 'Aprobada') this.notificaciones.exito('Justificación aprobada', `${justificacion.estudianteNombre} · la falta queda justificada.`);
    else this.notificaciones.aviso('Justificación observada', `Se devolvió la solicitud de ${justificacion.estudianteNombre}.`);
  }

  private normalizar(texto: string): string {
    return texto.trim().normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase();
  }
}
