import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AlumnoService } from '../alumno.service';

import { ContadorDirective } from '../../../shared/directives/contador.directive';

@Component({
  selector: 'app-inicio-alumno',
  standalone: true,
  imports: [CommonModule, RouterLink, ContadorDirective],
  templateUrl: './inicio-alumno.component.html',
  styleUrl: './inicio-alumno.component.css'
})
export class InicioAlumnoComponent {
  readonly alumno = inject(AlumnoService);
  /** Se recalculan al llegar los datos del backend. */
  get resumen() { return this.alumno.resumen(); }
  get recientes() { return this.alumno.historial({ hasta: this.alumno.hoy }).slice(0, 6); }

  get nombre(): string {
    return this.alumno.estudiante?.nombre ?? 'Estudiante';
  }

  get primerNombre(): string {
    return this.nombre.split(' ')[0];
  }

  get estadoHoy(): string {
    return this.alumno.etiquetaEstado(this.alumno.estadoHoy());
  }

  // Formateador de fecha principal (Ej: "Lunes, 28 de Septiembre 2026")
  formatearFechaLarga(fechaStr: string | Date): string {
    const fecha = new Date(typeof fechaStr === 'string' && fechaStr.length === 10 ? fechaStr + 'T12:00:00' : fechaStr);
    if (isNaN(fecha.getTime())) return '';

    const dias = ['Domingo', 'Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado'];
    const meses = [
      'Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio',
      'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre'
    ];

    return `${dias[fecha.getDay()]}, ${fecha.getDate()} de ${meses[fecha.getMonth()]} ${fecha.getFullYear()}`;
  }

  // Formateador corto para la tabla (Ej: "28 SEP 2026")
  formatearFechaCorta(fechaStr: string | Date): string {
    const fecha = new Date(typeof fechaStr === 'string' && fechaStr.length === 10 ? fechaStr + 'T12:00:00' : fechaStr);
    if (isNaN(fecha.getTime())) return '';

    const meses = ['ENE', 'FEB', 'MAR', 'ABR', 'MAY', 'JUN', 'JUL', 'AGO', 'SEP', 'OCT', 'NOV', 'DIC'];
    return `${fecha.getDate()} ${meses[fecha.getMonth()]} ${fecha.getFullYear()}`;
  }
}