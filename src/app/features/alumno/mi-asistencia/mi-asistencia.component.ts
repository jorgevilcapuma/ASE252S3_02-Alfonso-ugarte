import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AlumnoService, DiaAlumno } from '../alumno.service';
import { EstadoAsistencia } from '../../../core/models/asistencia.model';

@Component({
  selector: 'app-mi-asistencia',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './mi-asistencia.component.html',
  styleUrl: './mi-asistencia.component.css'
})
export class MiAsistenciaComponent {
  private readonly route = inject(ActivatedRoute);
  readonly alumno = inject(AlumnoService);

  periodo = '20';
  desde = '';
  hasta = this.alumno.hoy;
  fechaConsulta = this.alumno.hoy;
  idCursoSeleccionado = '';

  constructor() {
    this.route.queryParamMap.subscribe(
      params => (this.idCursoSeleccionado = params.get('curso') ?? '')
    );
  }

  get estudiante() {
    return this.alumno.estudiante;
  }

  get cursos() {
    return this.alumno.cursos;
  }

  get cursoSeleccionado() {
    return this.alumno.cursoPorId(this.idCursoSeleccionado);
  }

  get tituloRegistro(): string {
    return this.cursoSeleccionado
      ? `Asistencia · ${this.cursoSeleccionado.nombre}`
      : 'Mi asistencia';
  }

  get opcionesHistorial() {
    return {
      cursoId: this.idCursoSeleccionado || undefined,
      desde: this.desde || undefined,
      hasta: this.hasta || this.alumno.hoy
    };
  }

  get historial(): DiaAlumno[] {
    return this.alumno.historial(this.opcionesHistorial);
  }

  get resumen() {
    return this.alumno.resumen(this.opcionesHistorial);
  }

  get justificaciones() {
    return this.alumno.justificaciones;
  }

  get fechas(): string[] {
    return [...new Set(this.historial.map(item => item.fecha))];
  }

  get registrosFecha(): DiaAlumno[] {
    return this.historial.filter(item => item.fecha === this.fechaConsulta);
  }

  get estadoFechaSeleccionada(): EstadoAsistencia {
    return this.alumno.estadoEnFecha(
      this.fechaConsulta,
      this.idCursoSeleccionado || undefined
    );
  }

  cambiarPeriodo(): void {
    const dias: Record<string, number> = { '20': 28, '60': 84, '90': 126 };
    if (this.periodo === 'rango') return;

    const inicio = new Date(`${this.alumno.hoy}T12:00:00`);
    inicio.setDate(inicio.getDate() - (dias[this.periodo] ?? 28));
    this.desde = this.fechaIso(inicio);
    this.fechaConsulta = this.alumno.hoy;
  }

  cambiarCurso(): void {
    this.fechaConsulta = this.alumno.hoy;
  }

  etiquetaEstado(estado: EstadoAsistencia): string {
    return this.alumno.etiquetaEstado(estado);
  }

  claseEstado(estado: EstadoAsistencia): string {
    return `student-day-${estado}`;
  }

  iniciales(nombre: string): string {
    return nombre
      .split(' ')
      .slice(0, 2)
      .map(parte => parte[0])
      .join('')
      .toUpperCase();
  }

  formatearFechaLarga(fechaStr: string | Date): string {
    const fecha = new Date(`${fechaStr}T12:00:00`);
    if (isNaN(fecha.getTime())) return '';

    const diasSemana = ['Domingo', 'Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado'];
    const meses = [
      'Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio',
      'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre'
    ];

    return `${diasSemana[fecha.getDay()]}, ${fecha.getDate()} de ${meses[fecha.getMonth()]} ${fecha.getFullYear()}`;
  }

  formatearFechaCorta(fechaStr: string | Date): string {
    const fecha = new Date(`${fechaStr}T12:00:00`);
    if (isNaN(fecha.getTime())) return '';

    const meses = ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'sep', 'oct', 'nov', 'dic'];
    return `${fecha.getDate()} ${meses[fecha.getMonth()]} ${fecha.getFullYear()}`;
  }

  obtenerDiaMes(fechaStr: string | Date): { dia: number; mes: string } {
    const fecha = new Date(`${fechaStr}T12:00:00`);
    if (isNaN(fecha.getTime())) return { dia: 0, mes: '' };

    const meses = ['ENE', 'FEB', 'MAR', 'ABR', 'MAY', 'JUN', 'JUL', 'AGO', 'SEP', 'OCT', 'NOV', 'DIC'];
    return { dia: fecha.getDate(), mes: meses[fecha.getMonth()] };
  }

  private fechaIso(fecha: Date): string {
    return `${fecha.getFullYear()}-${String(fecha.getMonth() + 1).padStart(2, '0')}-${String(fecha.getDate()).padStart(2, '0')}`;
  }
}