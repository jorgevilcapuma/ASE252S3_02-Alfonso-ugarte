import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { ApiService } from '../../../core/services/api.service';
import { AsistenciaService } from '../../../core/services/asistencia.service';
import { ModoDatosService } from '../../../core/services/modo-datos.service';
import { NotificacionService } from '../../../core/services/notificacion.service';
import { IconoComponent } from '../../../shared/components/icono/icono.component';
import { CampoGestion, ConfigGestion, Formulario, GESTION, Opciones, Registro } from './gestion.config';

type FiltroEstado = 'A' | 'I' | 'T';

/**
 * CRUD de una tabla maestra del backend (docentes, estudiantes, cursos o grados y secciones), definida en gestion.config.ts:
 * listar con filtro de estado, crear, editar, eliminar lógico y restaurar.
 */
@Component({
  selector: 'app-gestion',
  standalone: true,
  imports: [FormsModule, IconoComponent],
  templateUrl: './gestion.component.html',
  styleUrl: './gestion.component.css'
})
export class GestionComponent {
  private readonly api = inject(ApiService);
  private readonly data = inject(AsistenciaService);
  private readonly notificaciones = inject(NotificacionService);
  readonly modo = inject(ModoDatosService);
  readonly config: ConfigGestion = GESTION[inject(ActivatedRoute).snapshot.data['entidad'] as string];

  readonly registros = signal<Registro[]>([]);
  readonly estado = signal<FiltroEstado>('A');
  readonly busqueda = signal('');
  readonly cargando = signal(true);
  readonly filtrados = computed(() => {
    const termino = this.normalizar(this.busqueda());
    return this.registros().filter(r => !termino || this.normalizar(this.config.buscar(r)).includes(termino));
  });

  opciones: Opciones = { aulas: [], docentes: [] };
  formulario: Formulario | null = null;
  editandoId: number | null = null;
  guardando = false;
  porEliminar: Registro | null = null;

  constructor() {
    if (!this.modo.esLocal) void this.cargar();
    else this.cargando.set(false);
  }

  async cargar(): Promise<void> {
    this.cargando.set(true);
    try {
      this.registros.set(await this.api.get<Registro[]>(this.config.ruta, { estado: this.estado() }));
    } catch (error) {
      this.notificaciones.error(`No se pudo cargar ${this.config.titulo.toLowerCase()}`, ApiService.mensaje(error));
    }
    this.cargando.set(false);
  }

  filtrarEstado(estado: FiltroEstado): void {
    this.estado.set(estado);
    void this.cargar();
  }

  opcionesDe(campo: CampoGestion): { valor: string; etiqueta: string }[] {
    return typeof campo.opciones === 'string' ? this.opciones[campo.opciones] : campo.opciones ?? [];
  }

  async abrir(registro?: Registro): Promise<void> {
    await this.cargarOpciones();
    this.editandoId = registro?.id ?? null;
    this.formulario = Object.fromEntries(this.config.campos.map(c => {
      const valor = registro?.[c.clave];
      const inicial = c.tipo === 'select' && c.requerido && !registro ? this.opcionesDe(c)[0]?.valor ?? '' : '';
      return [c.clave, valor === null || valor === undefined ? inicial : String(valor)];
    }));
  }

  async guardar(): Promise<void> {
    const f = this.formulario;
    if (!f || this.guardando) return;
    const faltante = this.config.campos.find(c => c.requerido && !f[c.clave]?.trim());
    if (faltante) {
      this.notificaciones.error('Faltan datos', `Completa el campo "${faltante.etiqueta}".`);
      return;
    }
    this.guardando = true;
    try {
      const cuerpo = this.config.cuerpo(f);
      const teniaCuenta = Boolean(this.registros().find(r => r.id === this.editandoId)?.['usuarioId']);
      const guardado = this.editandoId
        ? await this.api.put<Registro>(`${this.config.ruta}/${this.editandoId}`, cuerpo)
        : await this.api.post<Registro>(this.config.ruta, cuerpo);
      this.notificaciones.exito(this.editandoId ? 'Cambios guardados' : 'Registro creado', this.mensajeCuenta(guardado, teniaCuenta));
      this.formulario = null;
      await Promise.all([this.cargar(), this.data.recargarEstructura()]);
    } catch (error) {
      this.notificaciones.error('No se pudo guardar', ApiService.mensaje(error));
    }
    this.guardando = false;
  }

  async cambiarEstado(registro: Registro, accion: 'eliminar' | 'restaurar'): Promise<void> {
    this.porEliminar = null;
    try {
      await this.api.patch(`${this.config.ruta}/${registro.id}/${accion}`);
      this.notificaciones.exito(accion === 'eliminar' ? 'Registro eliminado' : 'Registro restaurado',
        accion === 'eliminar' ? 'Quedó inactivo; puedes restaurarlo desde el filtro Inactivos.' : 'Vuelve a estar activo.');
      await Promise.all([this.cargar(), this.data.recargarEstructura()]);
    } catch (error) {
      this.notificaciones.error(accion === 'eliminar' ? 'No se pudo eliminar' : 'No se pudo restaurar', ApiService.mensaje(error));
    }
  }

  /** Indica con qué datos ingresa la persona al portal después de guardar. */
  private mensajeCuenta(r: Registro, teniaCuenta: boolean): string {
    const cuenta = this.config.conCuenta;
    if (!cuenta) return this.config.mensajeGuardado?.(r) ?? `${this.config.titulo}: datos actualizados.`;
    if (r['usuarioId'] && !teniaCuenta) return `Cuenta creada: ingresa como ${cuenta.perfil} con ${String(r['correo'])} y la contraseña 123456.`;
    if (r['usuarioId']) return `Ingresa como ${cuenta.perfil} con ${String(r['correo'])}.`;
    return cuenta.sinCorreo;
  }

  nombre(r: Registro): string {
    return this.config.columnas[0].principal(r);
  }

  iniciales(texto: string): string {
    return texto.split(/[\s,]+/).filter(Boolean).slice(0, 2).map(parte => parte[0]).join('').toUpperCase();
  }

  /** Aulas y docentes activos para las listas desplegables. */
  private async cargarOpciones(): Promise<void> {
    try {
      if (this.config.necesita.includes('aulas')) {
        const aulas = await this.api.get<Registro[]>('/grados-secciones', { estado: 'A' });
        this.opciones.aulas = aulas.map(a => ({ valor: String(a.id), etiqueta: String(a['nombre']) }));
      }
      if (this.config.necesita.includes('docentes')) {
        const docentes = await this.api.get<Registro[]>('/docentes', { estado: 'A', cargo: 'Docente' });
        this.opciones.docentes = docentes.map(d => ({ valor: String(d.id), etiqueta: String(d['nombreCompleto']) }));
      }
    } catch (error) {
      this.notificaciones.error('No se pudieron cargar las opciones', ApiService.mensaje(error));
    }
  }

  private normalizar(texto: string): string {
    return texto.trim().normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase();
  }
}
