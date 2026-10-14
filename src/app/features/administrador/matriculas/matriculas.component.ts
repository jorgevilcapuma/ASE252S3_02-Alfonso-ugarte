import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Estudiante, NivelEducativo } from '../../../core/models/asistencia.model';
import { AsistenciaService } from '../../../core/services/asistencia.service';
import { NotificacionService } from '../../../core/services/notificacion.service';
import { IconoComponent } from '../../../shared/components/icono/icono.component';
import { ContadorDirective } from '../../../shared/directives/contador.directive';

type Formulario = Omit<Estudiante, 'id'>;

const POR_PAGINA = 12;

@Component({
  selector: 'app-matriculas',
  standalone: true,
  imports: [FormsModule, IconoComponent, ContadorDirective],
  templateUrl: './matriculas.component.html',
  styleUrl: './matriculas.component.css'
})
export class MatriculasComponent {
  readonly data = inject(AsistenciaService);
  private readonly notificaciones = inject(NotificacionService);

  /** Cambia con cada carga o modificación del padrón en el servicio (backend). */
  private readonly version = this.data.version;
  guardando = false;

  readonly busqueda = signal('');
  readonly nivel = signal<NivelEducativo | ''>('');
  readonly grado = signal('');
  readonly seccion = signal('');
  readonly pagina = signal(1);

  readonly filtrados = computed(() => {
    this.version();
    const texto = this.busqueda().trim().toLowerCase();
    return this.data.estudiantes.filter(estudiante =>
      (!this.nivel() || estudiante.nivel === this.nivel()) &&
      (!this.grado() || estudiante.grado === this.grado()) &&
      (!this.seccion() || estudiante.seccion === this.seccion()) &&
      (!texto || estudiante.nombre.toLowerCase().includes(texto) || estudiante.dni.includes(texto))
    );
  });
  readonly totalPaginas = computed(() => Math.max(1, Math.ceil(this.filtrados().length / POR_PAGINA)));
  readonly visibles = computed(() => {
    const pagina = Math.min(this.pagina(), this.totalPaginas());
    return this.filtrados().slice((pagina - 1) * POR_PAGINA, pagina * POR_PAGINA);
  });
  readonly totalPorNivel = computed(() => {
    this.version();
    return this.data.niveles.map(nivel => ({ nivel, total: this.data.estudiantesPorNivel(nivel).length }));
  });

  modalAbierto = false;
  trasladando: Estudiante | null = null;
  porRetirar: Estudiante | null = null;
  error = '';
  formulario: Formulario = this.formularioVacio();

  get total(): number { this.version(); return this.data.estudiantes.length; }

  gradosDe(nivel: NivelEducativo | ''): string[] {
    return nivel ? this.data.gradosPorNivel[nivel] : [];
  }

  etiquetaGrado(estudiante: Pick<Estudiante, 'nivel' | 'grado'>): string {
    return estudiante.nivel === 'Inicial' ? estudiante.grado : `${estudiante.grado}°`;
  }

  cambiarNivel(nivel: NivelEducativo | ''): void {
    this.nivel.set(nivel);
    this.grado.set('');
    this.pagina.set(1);
  }

  filtrar(campo: 'busqueda' | 'grado' | 'seccion', valor: string): void {
    this[campo].set(valor);
    this.pagina.set(1);
  }

  abrirMatricula(): void {
    this.trasladando = null;
    this.formulario = this.formularioVacio();
    this.error = '';
    this.modalAbierto = true;
  }

  abrirTraslado(estudiante: Estudiante): void {
    this.trasladando = estudiante;
    this.formulario = { ...estudiante };
    this.error = '';
    this.modalAbierto = true;
  }

  alCambiarNivelFormulario(): void {
    this.formulario.grado = this.data.gradosPorNivel[this.formulario.nivel][0];
  }

  async guardar(): Promise<void> {
    if (this.guardando) return;
    this.guardando = true;
    this.error = '';
    try {
      if (this.trasladando) {
        const { nivel, grado, seccion } = this.formulario;
        const error = await this.data.trasladarEstudiante(this.trasladando, nivel, grado, seccion);
        if (error) {
          this.error = error;
          return;
        }
        this.notificaciones.exito('Traslado registrado', `${this.trasladando.nombre} → ${nivel} ${this.etiquetaGrado(this.formulario)} "${seccion}"`);
      } else {
        const resultado = await this.data.matricularEstudiante(this.formulario);
        if (typeof resultado === 'string') {
          this.error = resultado;
          return;
        }
        this.notificaciones.exito('Matrícula registrada', `${resultado.nombre} · ${resultado.nivel} ${this.etiquetaGrado(resultado)} "${resultado.seccion}"`);
      }
      this.modalAbierto = false;
    } finally {
      this.guardando = false;
    }
  }

  async confirmarRetiro(): Promise<void> {
    const estudiante = this.porRetirar;
    if (!estudiante) return;
    this.porRetirar = null;
    const resultado = await this.data.retirarEstudiante(estudiante);
    if (resultado.ok) this.notificaciones.aviso('Matrícula anulada', `${estudiante.nombre}. ${resultado.mensaje}`);
    else this.notificaciones.error('No se pudo anular', resultado.mensaje);
  }

  exportarCsv(): void {
    const filas = [['DNI', 'Nombre', 'Nivel', 'Grado', 'Sección'], ...this.filtrados().map(e => [e.dni, e.nombre, e.nivel, e.grado, e.seccion])];
    const csv = '﻿' + filas.map(fila => fila.map(celda => `"${celda.replace(/"/g, '""')}"`).join(';')).join('\n');
    const enlace = document.createElement('a');
    enlace.href = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }));
    enlace.download = `matriculas-${this.data.hoy}.csv`;
    enlace.click();
    URL.revokeObjectURL(enlace.href);
    this.notificaciones.info('Padrón exportado', `${this.filtrados().length} estudiantes en CSV.`);
  }

  private formularioVacio(): Formulario {
    return { nombre: '', dni: '', genero: '', nivel: 'Primaria', grado: '1', seccion: 'A' };
  }
}
