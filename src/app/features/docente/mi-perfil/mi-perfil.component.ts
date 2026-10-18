import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ModoDatosService } from '../../../core/services/modo-datos.service';
import { NotificacionService } from '../../../core/services/notificacion.service';
import { DocenteService, PerfilDocente } from '../docente.service';

@Component({
  selector: 'app-mi-perfil',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './mi-perfil.component.html',
  styleUrls: ['./mi-perfil.component.css']
})
export class MiPerfilComponent {
  private readonly docenteService = inject(DocenteService);
  private readonly notificaciones = inject(NotificacionService);
  /** En modo servidor la ficha del docente solo la edita el administrador (Docentes y personal). */
  readonly modo = inject(ModoDatosService);

  /** Copia editable de la ficha del docente; se guarda en el backend al confirmar. */
  readonly docente = signal<PerfilDocente>({ ...this.docenteService.perfilDocente() });
  readonly guardadoExitoso = signal(false);
  guardando = false;
  error = '';

  constructor() {
    void this.docenteService.asegurar().then(() => this.docente.set({ ...this.docenteService.perfilDocente() }));
  }

  get iniciales(): string {
    const { nombres, apellidos } = this.docente();
    return `${nombres.trim()[0] ?? ''}${apellidos.trim()[0] ?? ''}`.toUpperCase();
  }

  async guardarCambios() {
    const perfil = this.docente();
    const telefono = perfil.telefono.replace(/\s+/g, ' ').trim();
    if (perfil.nombres.trim().length < 2 || perfil.apellidos.trim().length < 2) {
      this.error = 'Ingresa nombres y apellidos.';
      return;
    }
    if (!/^\+?[\d ]{9,15}$/.test(telefono)) {
      this.error = 'Ingresa un teléfono válido (9 dígitos como mínimo).';
      return;
    }
    this.error = '';
    this.guardando = true;
    const error = await this.docenteService.guardarPerfil({ ...perfil, nombres: perfil.nombres.trim(), apellidos: perfil.apellidos.trim(), telefono });
    this.guardando = false;
    if (error) {
      this.error = error;
      return;
    }
    this.docente.set({ ...this.docenteService.perfilDocente() });
    this.notificaciones.exito('Perfil actualizado', 'Tus datos se guardaron correctamente.');
    this.guardadoExitoso.set(true);
    setTimeout(() => this.guardadoExitoso.set(false), 3000);
  }
}
