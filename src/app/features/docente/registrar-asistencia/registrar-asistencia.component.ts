import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { ApiService } from '../../../core/services/api.service';
import { NotificacionService } from '../../../core/services/notificacion.service';
import { DocenteService, RegistroAlumnoCurso } from '../docente.service';

interface FilaAsistencia {
  alumnoId: string;
  nombres: string;
  apellidos: string;
  dni: string;
  estado: RegistroAlumnoCurso['estado'];
  observacion: string;
}

@Component({
  selector: 'app-registrar-asistencia',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './registrar-asistencia.component.html',
  styleUrl: './registrar-asistencia.component.css'
})
export class RegistrarAsistenciaComponent {
  private readonly route = inject(ActivatedRoute);
  readonly docenteService = inject(DocenteService);
  private readonly notificaciones = inject(NotificacionService);

  cursoId = '';
  fecha = this.docenteService.hoy;
  guardado = false;
  guardando = false;
  yaRegistrado = false;
  filas: FilaAsistencia[] = [];
  /** Evita que una respuesta lenta de otro curso/fecha reemplace la lista visible. */
  private consulta = 0;

  constructor() {
    this.route.queryParamMap.subscribe(async params => {
      await this.docenteService.asegurar();
      const curso = params.get('curso');
      this.cursoId = curso && this.docenteService.cursos().some(item => item.id === curso) ? curso : (this.docenteService.cursos()[0]?.id ?? '');
      const fecha = params.get('fecha');
      if (fecha && /^\d{4}-\d{2}-\d{2}$/.test(fecha) && fecha <= this.docenteService.hoy) this.fecha = fecha;
      await this.cargarAlumnos();
    });
  }

  /** Alumnos de la sección del curso con lo ya registrado en esa fecha (o "asistio" si aún no hay registro). */
  async cargarAlumnos(): Promise<void> {
    if (!this.cursoId || !this.fecha) {
      this.filas = [];
      return;
    }
    const consulta = ++this.consulta;
    try {
      const sesion = await this.docenteService.obtenerSesion(this.cursoId, this.fecha);
      if (consulta !== this.consulta) return;
      this.yaRegistrado = sesion.yaRegistrado;
      this.filas = sesion.filas.map(fila => ({ ...fila }));
    } catch (error) {
      if (consulta === this.consulta) this.notificaciones.error('No se pudo cargar el curso', ApiService.mensaje(error));
    }
  }

  marcarTodos(estado: 'asistio' | 'tardanza' | 'falta') {
    this.filas.forEach(f => (f.estado = estado));
  }

  contar(estado: FilaAsistencia['estado']): number {
    return this.filas.filter(f => f.estado === estado).length;
  }

  async guardarAsistencia() {
    if (this.fecha > this.docenteService.hoy) {
      this.notificaciones.error('Fecha no válida', 'No se puede registrar asistencia de una fecha futura.');
      return;
    }
    if (this.guardando) return;
    const registro = this.filas.reduce<Record<string, RegistroAlumnoCurso>>((acc, fila) => {
      acc[fila.alumnoId] = { estado: fila.estado, observacion: fila.observacion.trim() };
      return acc;
    }, {});
    this.guardando = true;
    const error = await this.docenteService.guardarRegistroCurso(this.cursoId, this.fecha, registro);
    this.guardando = false;
    if (error) {
      this.notificaciones.error('No se guardó la asistencia', error);
      return;
    }
    this.notificaciones.exito(
      this.yaRegistrado ? 'Asistencia actualizada' : 'Asistencia guardada',
      `${this.contar('asistio')} asistieron · ${this.contar('tardanza')} tardanzas · ${this.contar('falta')} faltas`
    );
    this.yaRegistrado = true;
    this.guardado = true;
    setTimeout(() => (this.guardado = false), 3500);
  }
}
