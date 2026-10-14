import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AlumnoService } from '../alumno.service';
import { AsistenciaService } from '../../../core/services/asistencia.service';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-mi-perfil',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './mi-perfil.component.html',
  styleUrl: './mi-perfil.component.css'
})
export class MiPerfilComponent {
  readonly alumno = inject(AlumnoService);
  readonly auth = inject(AuthService);
  private readonly asistencia = inject(AsistenciaService);
  contacto = { ...this.alumno.contacto };
  mensaje = '';
  guardando = false;

  constructor() {
    void this.alumno.asegurar().then(() => (this.contacto = { ...this.alumno.contacto }));
  }

  async guardarContacto(): Promise<void> {
    this.guardando = true;
    const error = await this.alumno.guardarContacto(this.contacto);
    this.guardando = false;
    this.mensaje = error ?? 'Datos de contacto guardados.';
    window.setTimeout(() => (this.mensaje = ''), 4000);
  }

  get codigo(): string {
    return this.alumno.estudiante?.id ?? 'Sin asignar';
  }

  get turno(): string {
    const seccion = this.alumno.estudiante?.seccion;
    return seccion ? this.asistencia.turnoSeccion(seccion) : '—';
  }

  obtenerIniciales(nombre: string): string {
    return nombre
      .split(' ')
      .slice(0, 2)
      .map(parte => parte[0])
      .join('')
      .toUpperCase();
  }
}
