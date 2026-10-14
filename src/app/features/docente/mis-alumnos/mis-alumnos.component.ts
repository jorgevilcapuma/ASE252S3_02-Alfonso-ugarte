import { Component, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DocenteService } from '../docente.service';

interface Alumno {
  id: string;
  dni: string;
  apellidos: string;
  nombres: string;
  genero?: 'M' | 'F';
  nivel: 'Inicial' | 'Primaria' | 'Secundaria';
  gradoSeccion: string;
  curso: string;
  asistencia: number;
  estado: 'Normal' | 'En Riesgo' | 'Crítico';
}

@Component({
  selector: 'app-mis-alumnos',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './mis-alumnos.component.html',
  styleUrls: ['./mis-alumnos.component.css']
})
export class MisAlumnosComponent {
  private docenteService = inject(DocenteService);

  // Filtros
  nivelSeleccionado = signal<string>('TODOS');
  seccionSeleccionada = signal<string>('TODOS');
  generoSeleccionado = signal<string>('TODOS');
  busqueda = signal<string>('');

  constructor() {
    void this.docenteService.asegurar();
  }

  /** Alumnos matriculados en los cursos del docente, con su porcentaje de asistencia calculado en el backend. */
  alumnosOriginales = computed<Alumno[]>(() => this.docenteService.alumnos().map(a => ({
    id: `${a.cursoId}-${a.id}`,
    dni: a.dni,
    apellidos: a.apellidos,
    nombres: a.nombres,
    genero: a.genero,
    nivel: a.nivel ?? 'Primaria',
    gradoSeccion: a.gradoSeccion ?? '',
    curso: a.curso ?? a.cursoNombre,
    asistencia: a.porcentajeAsistencia,
    estado: a.estadoGeneral
  })));

  // Lista de secciones disponibles según el nivel seleccionado
  seccionesDisponibles = computed(() => {
    const nivel = this.nivelSeleccionado();
    const lista = this.alumnosOriginales();
    const filtrados = nivel === 'TODOS' ? lista : lista.filter(a => a.nivel === nivel);
    const secciones = Array.from(new Set(filtrados.map(a => a.gradoSeccion)));
    return secciones.sort();
  });

  // Filtro computado multicriterio
  alumnosFiltrados = computed(() => {
    const niv = this.nivelSeleccionado();
    const sec = this.seccionSeleccionada();
    const gen = this.generoSeleccionado();
    const q = this.busqueda().toLowerCase().trim();

    return this.alumnosOriginales().filter(a => {
      const coincideNivel = niv === 'TODOS' || a.nivel === niv;
      const coincideSeccion = sec === 'TODOS' || a.gradoSeccion === sec;
      const coincideGenero = gen === 'TODOS' || a.genero === gen;
      const coincideBusqueda = !q ||
        a.nombres.toLowerCase().includes(q) ||
        a.apellidos.toLowerCase().includes(q) ||
        a.dni.includes(q);

      return coincideNivel && coincideSeccion && coincideGenero && coincideBusqueda;
    });
  });

  actualizarBusqueda(e: Event) {
    this.busqueda.set((e.target as HTMLInputElement).value);
  }

  cambiarNivel(e: Event) {
    this.nivelSeleccionado.set((e.target as HTMLSelectElement).value);
    this.seccionSeleccionada.set('TODOS'); // Resetear sección al cambiar nivel
  }

  cambiarSeccion(e: Event) {
    this.seccionSeleccionada.set((e.target as HTMLSelectElement).value);
  }

  cambiarGenero(e: Event) {
    this.generoSeleccionado.set((e.target as HTMLSelectElement).value);
  }
}
