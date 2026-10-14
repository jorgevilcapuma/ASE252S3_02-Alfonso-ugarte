import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ETIQUETA_PERFIL } from '../../../core/config/navegacion';
import { PerfilAcceso } from '../../../core/services/auth.service';
import { ApiService } from '../../../core/services/api.service';
import { NotificacionService } from '../../../core/services/notificacion.service';
import { EstadoUsuario, UsuarioSistema, UsuariosService } from '../../../core/services/usuarios.service';
import { IconoComponent } from '../../../shared/components/icono/icono.component';
import { ContadorDirective } from '../../../shared/directives/contador.directive';

interface FormularioUsuario { nombre: string; correo: string; perfil: PerfilAcceso; estado: EstadoUsuario; }

@Component({
  selector: 'app-usuarios',
  standalone: true,
  imports: [FormsModule, IconoComponent, ContadorDirective],
  templateUrl: './usuarios.component.html',
  styleUrl: './usuarios.component.css'
})
export class UsuariosComponent {
  private readonly servicio = inject(UsuariosService);
  private readonly notificaciones = inject(NotificacionService);

  readonly etiquetas = ETIQUETA_PERFIL;
  readonly perfiles: PerfilAcceso[] = ['administrador', 'docente', 'alumno'];

  readonly busqueda = signal('');
  readonly filtroPerfil = signal<PerfilAcceso | ''>('');

  readonly usuarios = this.servicio.usuarios;
  readonly filtrados = computed(() => {
    const texto = this.busqueda().trim().toLowerCase();
    const perfil = this.filtroPerfil();
    return this.usuarios().filter(usuario =>
      (!perfil || usuario.perfil === perfil) &&
      (!texto || usuario.nombre.toLowerCase().includes(texto) || usuario.correo.includes(texto))
    );
  });
  readonly totalActivos = computed(() => this.usuarios().filter(usuario => usuario.estado === 'Activo').length);
  readonly totalDocentes = computed(() => this.usuarios().filter(usuario => usuario.perfil === 'docente').length);
  readonly totalAlumnos = computed(() => this.usuarios().filter(usuario => usuario.perfil === 'alumno').length);

  constructor() {
    this.servicio.asegurar().catch(error => this.notificaciones.error('No se pudieron cargar los usuarios', ApiService.mensaje(error)));
  }

  guardando = false;
  modalAbierto = false;
  editandoId: string | null = null;
  porEliminar: UsuarioSistema | null = null;
  error = '';
  formulario: FormularioUsuario = this.formularioVacio();

  iniciales(nombre: string): string {
    return nombre.split(' ').slice(0, 2).map(parte => parte[0]).join('').toUpperCase();
  }

  abrirNuevo(): void {
    this.editandoId = null;
    this.formulario = this.formularioVacio();
    this.error = '';
    this.modalAbierto = true;
  }

  editar(usuario: UsuarioSistema): void {
    this.editandoId = usuario.id;
    this.formulario = { nombre: usuario.nombre, correo: usuario.correo, perfil: usuario.perfil, estado: usuario.estado };
    this.error = '';
    this.modalAbierto = true;
  }

  async guardar(): Promise<void> {
    if (this.guardando) return;
    this.guardando = true;
    const resultado = await this.servicio.guardarUsuario(this.formulario, this.editandoId ?? undefined);
    this.guardando = false;
    if (typeof resultado === 'string') {
      this.error = resultado;
      return;
    }
    this.notificaciones.exito(this.editandoId ? 'Usuario actualizado' : 'Usuario creado',
      `${resultado.nombre} · ${this.etiquetas[resultado.perfil]}${this.editandoId ? '' : ' · contraseña inicial 123456'}`);
    this.modalAbierto = false;
  }

  async alternar(usuario: UsuarioSistema): Promise<void> {
    const activo = usuario.estado !== 'Activo';
    const error = await this.servicio.alternarEstado(usuario.id);
    if (error) this.notificaciones.error('No se pudo cambiar el estado', error);
    else this.notificaciones[activo ? 'exito' : 'aviso'](activo ? 'Cuenta activada' : 'Cuenta desactivada', usuario.nombre);
  }

  async confirmarEliminar(): Promise<void> {
    const usuario = this.porEliminar;
    if (!usuario) return;
    this.porEliminar = null;
    const error = await this.servicio.eliminar(usuario.id);
    if (error) this.notificaciones.error('No se pudo eliminar', error);
    else this.notificaciones.aviso('Usuario eliminado', usuario.nombre);
  }

  private formularioVacio(): FormularioUsuario {
    return { nombre: '', correo: '', perfil: 'docente', estado: 'Activo' };
  }
}
