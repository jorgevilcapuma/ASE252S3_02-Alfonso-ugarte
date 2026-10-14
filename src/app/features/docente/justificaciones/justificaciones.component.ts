import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NotificacionService } from '../../../core/services/notificacion.service';
import { DocenteService } from '../docente.service';

@Component({
  selector: 'app-justificaciones',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './justificaciones.component.html',
  styleUrl: './justificaciones.component.css'
})
export class JustificacionesComponent {
  readonly docenteService = inject(DocenteService);
  private readonly notificaciones = inject(NotificacionService);

  get solicitudes() {
    return this.docenteService.justificaciones();
  }

  constructor() {
    void this.docenteService.asegurar();
  }

  async aprobar(id: string) {
    const error = await this.docenteService.aprobarJustificacion(id, 'Aprobada por el docente');
    if (error) this.notificaciones.error('No se pudo aprobar', error);
    else this.notificaciones.exito('Justificación aprobada', 'La falta quedó justificada.');
  }

  async rechazar(id: string) {
    const error = await this.docenteService.rechazarJustificacion(id, 'Sustento insuficiente');
    if (error) this.notificaciones.error('No se pudo observar', error);
    else this.notificaciones.aviso('Justificación observada', 'Se notificó al estudiante.');
  }
}
