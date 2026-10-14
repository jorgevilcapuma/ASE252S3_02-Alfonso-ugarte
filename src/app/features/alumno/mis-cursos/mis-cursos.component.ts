import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AlumnoService } from '../alumno.service';

@Component({
  selector: 'app-mis-cursos',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './mis-cursos.component.html',
  styleUrl: './mis-cursos.component.css'
})
export class MisCursosComponent {
  readonly alumno = inject(AlumnoService);

  get cursos() {
    return this.alumno.cursos;
  }

  get seccion(): string {
    return this.alumno.etiquetaSeccion;
  }

  porcentajeAsistencia(cursoId: string): number {
    return this.alumno.resumen({ cursoId }).porcentajeAsistencia;
  }
}