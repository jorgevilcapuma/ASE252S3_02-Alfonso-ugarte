import * as R from '../../../core/validacion/reglas';
import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AlumnoService } from '../alumno.service';
import { NotificacionService } from '../../../core/services/notificacion.service';
import { EstadoJustificacion, Justificacion } from '../../../core/models/asistencia.model';

@Component({
  selector: 'app-mis-justificaciones',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './mis-justificaciones.component.html',
  styleUrl: './mis-justificaciones.component.css'
})
export class MisJustificacionesComponent {
  readonly alumno = inject(AlumnoService);
  private readonly notificaciones = inject(NotificacionService);

  filtro: 'Todas' | EstadoJustificacion = 'Todas';
  mostrarFormulario = false;
  fecha = this.alumno.hoy;
  cursoId = '';
  motivo = '';
  archivoNombre = '';
  mensaje = '';
  tipoMensaje: 'error' | 'success' = 'error';
  enviando = false;

  get solicitudes(): Justificacion[] {
    return this.alumno.justificaciones.filter(
      item => this.filtro === 'Todas' || item.estado === this.filtro
    );
  }

  get pendientes(): number {
    return this.alumno.justificaciones.filter(item => item.estado === 'Pendiente').length;
  }

  get aprobadas(): number {
    return this.alumno.justificaciones.filter(item => item.estado === 'Aprobada').length;
  }

  get rechazadas(): number {
    return this.alumno.justificaciones.filter(item => item.estado === 'Observada').length;
  }

  abrirFormulario(): void {
    this.fecha = this.alumno.hoy;
    this.cursoId = '';
    this.motivo = '';
    this.archivoNombre = '';
    this.mensaje = '';
    this.mostrarFormulario = true;
  }

  seleccionarArchivo(event: Event): void {
    const input = event.target as HTMLInputElement;
    const archivo = input.files?.[0];
    if (!archivo) return;

    if (archivo.size > 5 * 1024 * 1024) {
      this.mostrarError('El archivo no debe superar 5 MB.');
      input.value = '';
      return;
    }

    if (!/\.(pdf|jpg|jpeg|png)$/i.test(archivo.name)) {
      this.mostrarError('Adjunta un archivo PDF, JPG o PNG.');
      input.value = '';
      return;
    }

    this.archivoNombre = archivo.name;
    this.mensaje = '';
  }

  async enviar(): Promise<void> {
    const invalido = R.primero(R.diaHabil(this.fecha), R.fechaNoFutura(this.fecha, this.alumno.hoy), R.texto(this.motivo, 5, 'El motivo'));
    if (invalido) {
      return this.mostrarError(invalido);
    }
    if (this.alumno.justificaciones.some(item => item.fecha === this.fecha && (item.cursoId ?? '') === this.cursoId && item.estado === 'Pendiente')) {
      return this.mostrarError('Ya existe una solicitud pendiente para esta fecha.');
    }
    this.enviando = true;
    const error = await this.alumno.solicitarJustificacion(this.fecha, this.motivo, this.archivoNombre || undefined, this.cursoId || undefined);
    this.enviando = false;
    if (error) {
      return this.mostrarError(error);
    }

    this.mostrarFormulario = false;
    this.mensaje = '';
    this.notificaciones.exito('Solicitud enviada', 'Queda pendiente de revisión del colegio.');
  }

  etiquetaEstado(estado: EstadoJustificacion): string {
    return estado === 'Observada' ? 'Rechazada' : estado;
  }

  formatearFecha(fechaStr: string | Date): string {
    const fecha = new Date(typeof fechaStr === 'string' && fechaStr.length === 10 ? fechaStr + 'T12:00:00' : fechaStr);
    if (isNaN(fecha.getTime())) return '';

    const meses = [
      'ene', 'feb', 'mar', 'abr', 'may', 'jun',
      'jul', 'ago', 'sep', 'oct', 'nov', 'dic'
    ];

    return `${fecha.getDate()} ${meses[fecha.getMonth()]} ${fecha.getFullYear()}`;
  }

  obtenerNombreCurso(cursoId?: string): string {
    if (!cursoId) return 'Asistencia general';
    const curso = this.alumno.cursos.find(c => c.id === cursoId);
    return curso ? curso.nombre : 'Asistencia general';
  }

  private mostrarError(mensaje: string): void {
    this.tipoMensaje = 'error';
    this.mensaje = mensaje;
  }
}