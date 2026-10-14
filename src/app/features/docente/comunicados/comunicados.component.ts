import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ComunicadoDocente, DocenteService } from '../docente.service';
import { IconoComponent } from '../../../shared/components/icono/icono.component';
import { NotificacionService } from '../../../core/services/notificacion.service';

@Component({
  selector: 'app-comunicados',
  standalone: true,
  imports: [CommonModule, FormsModule, IconoComponent],
  templateUrl: './comunicados.component.html',
  styleUrl: './comunicados.component.css'
})
export class ComunicadosComponent {
  readonly docenteService = inject(DocenteService);
  private readonly notificaciones = inject(NotificacionService);

  porEliminar: ComunicadoDocente | null = null;
  titulo = '';
  contenido = '';
  cursoSeleccionado = '';
  prioridad: 'Alta' | 'Media' | 'Baja' = 'Media';
  mensajeExito = false;
  publicando = false;

  constructor() {
    void this.docenteService.asegurar();
  }

  async publicarComunicado() {
    if (!this.titulo.trim() || !this.contenido.trim() || this.publicando) return;

    const curso = this.docenteService.cursos().find(c => c.id === this.cursoSeleccionado);

    this.publicando = true;
    const error = await this.docenteService.agregarComunicado({
      titulo: this.titulo,
      contenido: this.contenido,
      cursoId: this.cursoSeleccionado || undefined,
      cursoNombre: curso ? `${curso.nombre} - ${curso.grado} ${curso.seccion}` : 'Todos los Cursos',
      prioridad: this.prioridad
    });
    this.publicando = false;
    if (error) {
      this.notificaciones.error('No se publicó el comunicado', error);
      return;
    }

    // Resetear formulario
    this.titulo = '';
    this.contenido = '';
    this.cursoSeleccionado = '';
    this.prioridad = 'Media';

    this.notificaciones.exito('Comunicado publicado', 'Ya está visible para tus alumnos.');
    this.mensajeExito = true;
    setTimeout(() => (this.mensajeExito = false), 3000);
  }

  async eliminar() {
    const comunicado = this.porEliminar;
    if (!comunicado) return;
    this.porEliminar = null;
    const error = await this.docenteService.eliminarComunicado(comunicado.id);
    if (error) this.notificaciones.error('No se eliminó el comunicado', error);
    else this.notificaciones.aviso('Comunicado eliminado', comunicado.titulo);
  }
}
