import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AlumnoService } from '../alumno.service';

@Component({
  selector: 'app-mis-alertas',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './mis-alertas.component.html',
  styleUrl: './mis-alertas.component.css'
})
export class MisAlertasComponent {
  readonly alumno = inject(AlumnoService);

  get alertas() {
    return this.alumno.alertas;
  }

  formatearFecha(fechaStr: string | Date): string {
    const fecha = new Date(typeof fechaStr === 'string' && fechaStr.length === 10 ? fechaStr + 'T12:00:00' : fechaStr);
    if (isNaN(fecha.getTime())) return '';

    const meses = [
      'Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio',
      'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre'
    ];

    return `${fecha.getDate()} de ${meses[fecha.getMonth()]} ${fecha.getFullYear()}`;
  }
}