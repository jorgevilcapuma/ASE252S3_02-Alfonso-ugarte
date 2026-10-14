import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { AlumnoService } from '../alumno.service';

@Component({
  selector: 'app-comunicaciones-alumno',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './comunicaciones.component.html',
  styleUrl: './comunicaciones.component.css'
})
export class ComunicacionesComponent {

  readonly alumno = inject(AlumnoService);

  get comunicados() {
    return this.alumno.comunicaciones || [];
  }

  // Helpers para evitar dependencia de locale global 'es-PE' en el HTML
  obtenerDia(fecha: string | Date): string {
    const f = new Date(typeof fecha === 'string' && fecha.length === 10 ? fecha + 'T12:00:00' : fecha);
    return isNaN(f.getTime()) ? '' : f.getDate().toString();
  }

  obtenerMes(fecha: string | Date): string {
    const f = new Date(typeof fecha === 'string' && fecha.length === 10 ? fecha + 'T12:00:00' : fecha);
    if (isNaN(f.getTime())) return '';
    const meses = ['ENE', 'FEB', 'MAR', 'ABR', 'MAY', 'JUN', 'JUL', 'AGO', 'SEP', 'OCT', 'NOV', 'DIC'];
    return meses[f.getMonth()];
  }
}