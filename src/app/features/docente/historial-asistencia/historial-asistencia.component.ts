import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { DocenteService, SesionHistorial } from '../docente.service';

@Component({
  selector: 'app-historial-asistencia',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './historial-asistencia.component.html',
  styleUrl: './historial-asistencia.component.css'
})
export class HistorialAsistenciaComponent {
  readonly docenteService = inject(DocenteService);

  cursoFiltro = '';
  fechaInicio = '';
  fechaFin = '';

  /** Sesiones reales guardadas desde "Asistencia por curso". */
  get historialFiltrado(): SesionHistorial[] {
    return this.docenteService.historialSesiones().filter(item => {
      const coincideCurso = !this.cursoFiltro || item.cursoId === this.cursoFiltro;
      const coincideInicio = !this.fechaInicio || item.fecha >= this.fechaInicio;
      const coincideFin = !this.fechaFin || item.fecha <= this.fechaFin;
      return coincideCurso && coincideInicio && coincideFin;
    });
  }

  /** Asistió o llegó tarde: ambos cuentan como asistencia. */
  porcentaje(item: SesionHistorial): number {
    return item.totalAlumnos ? (item.asistieron + item.tardanzas) / item.totalAlumnos * 100 : 0;
  }

  limpiarFiltros() {
    this.cursoFiltro = '';
    this.fechaInicio = '';
    this.fechaFin = '';
  }
}
