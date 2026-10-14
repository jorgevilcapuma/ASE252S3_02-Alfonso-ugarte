import { Component, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { DocenteService } from '../docente.service';

@Component({
  selector: 'app-mis-cursos',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './mis-cursos.component.html',
  styleUrls: ['./mis-cursos.component.css']
})
export class MisCursosComponent {
  private docenteService = inject(DocenteService);

  // Filtro de nivel
  nivelSeleccionado = signal<'TODOS' | 'Inicial' | 'Primaria' | 'Secundaria'>('TODOS');

  // Obtener cursos desde el servicio
  todosLosCursos = this.docenteService.cursos;

  // Filtrado computado
  cursosFiltrados = computed(() => {
    const nivel = this.nivelSeleccionado();
    if (nivel === 'TODOS') {
      return this.todosLosCursos();
    }
    return this.todosLosCursos().filter(c => c.nivel === nivel);
  });

  cambiarNivel(nivel: 'TODOS' | 'Inicial' | 'Primaria' | 'Secundaria') {
    this.nivelSeleccionado.set(nivel);
  }
}