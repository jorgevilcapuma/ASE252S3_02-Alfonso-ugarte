import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../../core/services/api.service';
import { NotificacionService } from '../../../core/services/notificacion.service';
import { Alumno, DocenteService, ResumenMensualAlumno } from '../docente.service';

const MESES = ['Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio', 'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre'];

type FilaReporte = Alumno & { sesiones?: number };

@Component({
  selector: 'app-reportes',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './reportes.component.html',
  styleUrl: './reportes.component.css'
})
export class ReportesComponent {
  readonly docenteService = inject(DocenteService);
  private readonly notificaciones = inject(NotificacionService);

  cursoSeleccionado = '';
  /** Vacío = acumulado del año; si no, AAAA-MM calculado por el backend con las sesiones guardadas. */
  mesSeleccionado = '';
  /** Resumen del mes por "cursoId|alumnoId". */
  private resumenMes: Record<string, ResumenMensualAlumno> = {};

  /** Meses del año escolar (marzo a diciembre) hasta el mes actual. */
  readonly meses = (() => {
    const [anio, mesActual] = this.docenteService.hoy.split('-').map(Number);
    return MESES.map((nombre, i) => ({ valor: `${anio}-${String(i + 1).padStart(2, '0')}`, nombre: `${nombre} ${anio}` }))
      .slice(2, Math.max(3, mesActual));
  })();

  constructor() {
    void this.docenteService.asegurar();
  }

  /** Al elegir un mes se consulta el reporte de cada curso visible. */
  async actualizarMes(): Promise<void> {
    this.resumenMes = {};
    if (!this.mesSeleccionado) return;
    await this.docenteService.asegurar();
    const cursos = this.cursoSeleccionado ? [this.cursoSeleccionado] : this.docenteService.cursos().map(c => c.id);
    try {
      const reportes = await Promise.all(cursos.map(async id => ({ id, filas: await this.docenteService.resumenMensual(id, this.mesSeleccionado) })));
      const resumen: Record<string, ResumenMensualAlumno> = {};
      reportes.forEach(r => r.filas.forEach(fila => resumen[`${r.id}|${fila.alumnoId}`] = fila));
      this.resumenMes = resumen;
    } catch (error) {
      this.notificaciones.error('No se pudo generar el reporte', ApiService.mensaje(error));
    }
  }

  get alumnosReporte(): FilaReporte[] {
    const lista = this.docenteService.alumnosPorCurso(this.cursoSeleccionado);
    if (!this.mesSeleccionado) return lista;
    return lista
      .map(alumno => {
        const { sesiones = 0, porcentaje = 0 } = this.resumenMes[`${alumno.cursoId}|${alumno.id}`] ?? {};
        const estadoGeneral: Alumno['estadoGeneral'] = porcentaje >= 80 ? 'Normal' : porcentaje >= 68 ? 'En Riesgo' : 'Crítico';
        return { ...alumno, porcentajeAsistencia: porcentaje, estadoGeneral, sesiones };
      })
      .filter(alumno => alumno.sesiones > 0);
  }

  get resumenCurso() {
    const lista = this.alumnosReporte;
    if (lista.length === 0) return { total: 0, promedio: 0, enRiesgo: 0 };

    const suma = lista.reduce((acc, a) => acc + a.porcentajeAsistencia, 0);
    const enRiesgo = lista.filter(a => a.estadoGeneral !== 'Normal').length;

    return {
      total: lista.length,
      promedio: Math.round(suma / lista.length),
      enRiesgo
    };
  }

  exportarPDF() {
    window.print();
  }
}
