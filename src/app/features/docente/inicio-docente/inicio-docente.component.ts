import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AsistenciaService } from '../../../core/services/asistencia.service';
import { DocenteService } from '../docente.service';

import { ContadorDirective } from '../../../shared/directives/contador.directive';

@Component({
  selector: 'app-inicio-docente',
  standalone: true,
  imports: [CommonModule, RouterLink, ContadorDirective],
  templateUrl: './inicio-docente.component.html',
  styleUrl: './inicio-docente.component.css'
})
export class InicioDocenteComponent {
  readonly docenteService = inject(DocenteService);
  private readonly asistencia = inject(AsistenciaService);

  get docente() { return this.docenteService.perfilDocente(); }
  get cursos() { return this.docenteService.cursos(); }
  get alumnosEnRiesgo() { return this.docenteService.alumnos().filter(alumno => alumno.estadoGeneral !== 'Normal'); }

  /** Las mismas solicitudes que se revisan en Justificaciones (registro compartido con alumnos). */
  get justificacionesPendientes() {
    return this.asistencia.justificaciones
      .filter(item => item.estado === 'Pendiente')
      .map(item => {
        const estudiante = this.asistencia.estudiantes.find(e => e.id === item.estudianteId);
        const aula = estudiante ? `${estudiante.grado}${estudiante.nivel === 'Inicial' ? '' : '°'} "${estudiante.seccion}" ${estudiante.nivel}` : '';
        return { id: item.id, alumnoNombre: item.estudianteNombre, motivo: item.motivo, cursoNombre: aula };
      });
  }
}
