import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { AlumnoService } from '../alumno.service';

@Component({
  selector: 'app-mi-horario',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './mi-horario.component.html',
  styleUrl: './mi-horario.component.css'
})
export class MiHorarioComponent {
  readonly alumno = inject(AlumnoService);
  readonly dias = ['Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes'];
  readonly bloques = [
    { inicio: '08:00', fin: '09:15' },
    { inicio: '09:15', fin: '10:30' },
    { inicio: '10:30', fin: '11:00', recreo: true },
    { inicio: '11:00', fin: '12:30' },
    { inicio: '12:30', fin: '14:00' }
  ];

  get cursos() {
    return this.alumno.cursos;
  }

  get seccion(): string {
    return this.alumno.etiquetaSeccion;
  }

  cursoEn(dia: string, hora: string) {
    return this.cursos.find(curso =>
      curso.horario.some(sesion => sesion.dia === dia && sesion.inicio === hora)
    );
  }

  horaFin(dia: string, hora: string): string {
    return (
      this.cursoEn(dia, hora)?.horario.find(
        sesion => sesion.dia === dia && sesion.inicio === hora
      )?.fin ?? ''
    );
  }
}
