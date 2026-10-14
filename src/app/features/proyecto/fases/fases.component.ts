import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { IconoComponent } from '../../../shared/components/icono/icono.component';
import { AsistenciaService } from '../../../core/services/asistencia.service';

@Component({
  selector: 'app-fases',
  standalone: true,
  imports: [CommonModule, IconoComponent],
  templateUrl: './fases.component.html',
  styleUrl: './fases.component.css'
})
export class FasesComponent {
  private readonly data = inject(AsistenciaService);

  /** Fases registradas en la base de datos (tabla fase_proyecto). */
  get fases() { return this.data.fases; }
}
