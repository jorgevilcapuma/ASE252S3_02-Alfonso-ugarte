import { Component, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { NivelEducativo } from '../../../core/models/asistencia.model';
import { AsistenciaService } from '../../../core/services/asistencia.service';
import { NotificacionService } from '../../../core/services/notificacion.service';
import { IconoComponent } from '../../../shared/components/icono/icono.component';

interface TarjetaSeccion {
  seccion: string;
  turno: 'Mañana' | 'Tarde';
  estudiantes: number;
  registrados: number;
  faltas: number;
}

/** Paso previo a la matriz: elegir nivel, grado y sección del aula. */
@Component({
  selector: 'app-seleccion-seccion',
  standalone: true,
  imports: [RouterLink, IconoComponent],
  templateUrl: './seleccion-seccion.component.html',
  styleUrl: './seleccion-seccion.component.css'
})
export class SeleccionSeccionComponent {
  readonly data = inject(AsistenciaService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly notificaciones = inject(NotificacionService);

  nivel: NivelEducativo = 'Primaria';
  grado = '1';

  constructor() {
    void this.data.asegurarGestion().then(() => this.data.cargarAsistencia(this.data.hoy, this.data.hoy)).catch(() => undefined);
    this.route.queryParamMap.pipe(takeUntilDestroyed()).subscribe(params => {
      const nivel = params.get('nivel') as NivelEducativo | null;
      if (nivel && this.data.niveles.includes(nivel)) this.nivel = nivel;
      const grado = params.get('grado');
      this.grado = grado && this.data.gradosPorNivel[this.nivel].includes(grado) ? grado : this.data.gradosPorNivel[this.nivel][0];
    });
  }

  get grados(): string[] { return this.data.gradosPorNivel[this.nivel]; }
  get etiquetaAula(): string { return `${this.data.etiquetaGrado(this.nivel, this.grado)} de ${this.nivel}`; }

  get tarjetas(): TarjetaSeccion[] {
    return this.data.secciones.map(seccion => {
      const estudiantes = this.data.estudiantesDeAula(this.nivel, this.grado, seccion);
      const estados = estudiantes.map(estudiante => this.data.obtenerEstado(estudiante.id, this.data.hoy));
      return {
        seccion,
        turno: this.data.turnoSeccion(seccion),
        estudiantes: estudiantes.length,
        registrados: estados.filter(estado => estado !== 'pendiente').length,
        faltas: estados.filter(estado => estado === 'falta').length
      };
    });
  }

  elegir(nivel: NivelEducativo, grado?: string): void {
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { nivel, grado: grado ?? this.data.gradosPorNivel[nivel][0] },
      replaceUrl: true
    });
  }

  porcentaje(tarjeta: TarjetaSeccion): number {
    return tarjeta.estudiantes ? Math.round(tarjeta.registrados / tarjeta.estudiantes * 100) : 0;
  }

  solicitarActualizacion(): void {
    this.notificaciones.info('Solicitud enviada', 'El administrador revisará la estructura académica de este grado.');
  }
}
