import { Component, DestroyRef, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AsistenciaService } from '../../core/services/asistencia.service';
import { ModoDatosService } from '../../core/services/modo-datos.service';
import { NotificacionService } from '../../core/services/notificacion.service';
import { LOGO_POR_DEFECTO, TEMA_POR_DEFECTO, TemaService } from '../../core/services/tema.service';
import { UsuariosService } from '../../core/services/usuarios.service';
import { IconoComponent, NombreIcono } from '../../shared/components/icono/icono.component';

type Pestana = 'datos' | 'reglas' | 'usuarios' | 'estructura' | 'identidad';

const LADO_MAXIMO_LOGO = 320;

@Component({
  selector: 'app-configuracion',
  standalone: true,
  imports: [FormsModule, RouterLink, IconoComponent],
  templateUrl: './configuracion.component.html',
  styleUrl: './configuracion.component.css'
})
export class ConfiguracionComponent {
  readonly data = inject(AsistenciaService);
  readonly tema = inject(TemaService);
  readonly usuarios = inject(UsuariosService);
  private readonly notificaciones = inject(NotificacionService);
  /** En modo servidor la hora de entrada, la tolerancia y el plazo los fija el backend (application.yml). */
  readonly modo = inject(ModoDatosService);

  readonly pestanas: { id: Pestana; titulo: string; detalle: string; icono: NombreIcono }[] = [
    { id: 'datos', titulo: 'Datos del colegio', detalle: 'Información institucional general', icono: 'edificio' },
    { id: 'reglas', titulo: 'Reglas de asistencia', detalle: 'Entrada, tolerancia, alertas y plazos', icono: 'reloj' },
    { id: 'usuarios', titulo: 'Gestión de usuarios', detalle: 'Roles, permisos y cuentas', icono: 'usuarios' },
    { id: 'estructura', titulo: 'Estructura académica', detalle: 'Niveles, grados y secciones', icono: 'estructura' },
    { id: 'identidad', titulo: 'Identidad visual', detalle: 'Logotipo y colores institucionales', icono: 'paleta' }
  ];
  pestana: Pestana = 'datos';

  /** Copia editable: el backend solo cambia al pulsar "Guardar cambios". */
  formulario = this.copiaActual();
  errores: Partial<Record<'nombre' | 'codigo' | 'direccion' | 'colores' | 'logo', string>> = {};
  guardando = false;
  private guardado = false;

  /** Reglas de asistencia: se guardan aparte con su propio botón. */
  reglas = this.copiaReglas();
  guardandoReglas = false;

  constructor() {
    // Si se sale sin guardar, se descarta la vista previa de colores.
    inject(DestroyRef).onDestroy(() => { if (!this.guardado) this.tema.aplicar(this.tema.tema()); });
    // Datos vigentes del backend; si el formulario aún no se editó, se actualiza con ellos.
    const inicial = JSON.stringify(this.formulario);
    const reglasIniciales = JSON.stringify(this.reglas);
    void this.data.cargarInstitucion().then(() => {
      if (JSON.stringify(this.formulario) === inicial) this.formulario = this.copiaActual();
      if (JSON.stringify(this.reglas) === reglasIniciales) this.reglas = this.copiaReglas();
    });
    this.usuarios.asegurar().catch(() => undefined);
  }

  private copiaActual() {
    const tema = this.tema.tema();
    return {
      nombre: this.data.nombreInstitucion,
      codigo: this.data.codigoModular,
      direccion: this.data.direccionInstitucion,
      colorPrincipal: tema.colorPrincipal,
      colorSecundario: tema.colorSecundario,
      logo: tema.logo
    };
  }

  private copiaReglas() {
    const { horaEntrada, minutosTolerancia, faltasPreventivas, faltasCriticas, diasEdicionDocente } = this.data.reglas;
    return { horaEntrada, minutosTolerancia, faltasPreventivas, faltasCriticas, diasEdicionDocente };
  }

  get hayCambiosReglas(): boolean { return JSON.stringify(this.reglas) !== JSON.stringify(this.copiaReglas()); }

  /** Vista previa del límite de puntualidad con los valores del formulario. */
  get limiteVista(): string {
    const [h, m] = (this.reglas.horaEntrada || '00:00').split(':').map(Number);
    const total = h * 60 + m + (Number(this.reglas.minutosTolerancia) || 0);
    return `${String(Math.floor(total / 60) % 24).padStart(2, '0')}:${String(total % 60).padStart(2, '0')}`;
  }

  async guardarReglas(): Promise<void> {
    if (this.guardandoReglas) return;
    this.guardandoReglas = true;
    const error = await this.data.guardarReglas({
      ...this.reglas,
      minutosTolerancia: Number(this.reglas.minutosTolerancia), faltasPreventivas: Number(this.reglas.faltasPreventivas),
      faltasCriticas: Number(this.reglas.faltasCriticas), diasEdicionDocente: Number(this.reglas.diasEdicionDocente)
    });
    this.guardandoReglas = false;
    if (error) {
      this.notificaciones.error('No se pudieron guardar las reglas', error);
      return;
    }
    this.reglas = this.copiaReglas();
    this.notificaciones.exito('Reglas guardadas', 'Se aplican desde ahora en las marcaciones, las alertas y los plazos de corrección.');
  }

  get logoVista(): string { return this.formulario.logo || LOGO_POR_DEFECTO; }
  get hayCambios(): boolean {
    const t = this.tema.tema();
    return this.formulario.nombre !== this.data.nombreInstitucion || this.formulario.codigo !== this.data.codigoModular
      || this.formulario.direccion !== this.data.direccionInstitucion || this.formulario.colorPrincipal !== t.colorPrincipal
      || this.formulario.colorSecundario !== t.colorSecundario || this.formulario.logo !== t.logo;
  }
  get totalActivos(): number { return this.usuarios.usuarios().filter(usuario => usuario.estado === 'Activo').length; }
  contarPerfil(perfil: 'administrador' | 'docente' | 'alumno'): number { return this.usuarios.usuarios().filter(usuario => usuario.perfil === perfil).length; }

  previsualizarColores(): void {
    const { colorPrincipal, colorSecundario } = this.formulario;
    if (this.tema.esColorValido(colorPrincipal) && this.tema.esColorValido(colorSecundario)) {
      this.errores.colores = undefined;
      this.tema.aplicar({ colorPrincipal, colorSecundario });
    }
  }

  async elegirLogo(evento: Event): Promise<void> {
    const entrada = evento.target as HTMLInputElement;
    const archivo = entrada.files?.[0];
    entrada.value = '';
    if (!archivo) return;
    if (!['image/png', 'image/jpeg', 'image/webp'].includes(archivo.type)) {
      this.errores.logo = 'Usa una imagen PNG, JPG o WEBP.';
      return;
    }
    if (archivo.size > 2 * 1024 * 1024) {
      this.errores.logo = 'La imagen supera los 2 MB.';
      return;
    }
    try {
      this.formulario.logo = await this.reducirImagen(archivo);
      this.errores.logo = undefined;
    } catch {
      this.errores.logo = 'No se pudo leer la imagen.';
    }
  }

  quitarLogo(): void { this.formulario.logo = ''; }

  restablecerIdentidad(): void {
    this.formulario.colorPrincipal = TEMA_POR_DEFECTO.colorPrincipal;
    this.formulario.colorSecundario = TEMA_POR_DEFECTO.colorSecundario;
    this.formulario.logo = '';
    this.previsualizarColores();
  }

  async guardar(): Promise<void> {
    if (this.guardando) return;
    const nombre = this.formulario.nombre.trim().replace(/\s+/g, ' ');
    const direccion = this.formulario.direccion.trim();
    const codigo = this.formulario.codigo.trim();
    this.errores = {};
    if (nombre.length < 5) this.errores.nombre = 'Ingresa el nombre completo de la institución.';
    if (!/^\d{7}$/.test(codigo)) this.errores.codigo = 'El código modular tiene 7 dígitos.';
    if (direccion.length < 5) this.errores.direccion = 'Ingresa la dirección de la institución.';
    if (!this.tema.esColorValido(this.formulario.colorPrincipal) || !this.tema.esColorValido(this.formulario.colorSecundario)) {
      this.errores.colores = 'Usa colores hexadecimales de 6 dígitos, por ejemplo #9b1b1f.';
    }
    if (Object.values(this.errores).some(Boolean)) {
      this.pestana = this.errores.colores ? 'identidad' : 'datos';
      this.notificaciones.error('Revisa el formulario', 'Hay campos con datos no válidos.');
      return;
    }

    this.guardando = true;
    const error = await this.data.guardarConfiguracion({
      nombre, codigoModular: codigo, direccion,
      colorPrincipal: this.formulario.colorPrincipal.trim().toLowerCase(),
      colorSecundario: this.formulario.colorSecundario.trim().toLowerCase(),
      logo: this.formulario.logo
    });
    this.guardando = false;
    if (error) {
      this.notificaciones.error('No se pudo guardar', error);
      return;
    }
    this.formulario = this.copiaActual();
    this.guardado = true;
    this.notificaciones.exito('Cambios guardados', 'La configuración institucional se actualizó.');
  }

  /** Reduce el logotipo para no saturar el almacenamiento del navegador. */
  private reducirImagen(archivo: File): Promise<string> {
    return new Promise((resolver, rechazar) => {
      const lector = new FileReader();
      lector.onerror = () => rechazar(lector.error);
      lector.onload = () => {
        const imagen = new Image();
        imagen.onerror = () => rechazar(new Error('imagen'));
        imagen.onload = () => {
          const escala = Math.min(1, LADO_MAXIMO_LOGO / Math.max(imagen.width, imagen.height));
          const lienzo = document.createElement('canvas');
          lienzo.width = Math.round(imagen.width * escala);
          lienzo.height = Math.round(imagen.height * escala);
          lienzo.getContext('2d')?.drawImage(imagen, 0, 0, lienzo.width, lienzo.height);
          resolver(lienzo.toDataURL('image/png'));
        };
        imagen.src = String(lector.result);
      };
      lector.readAsDataURL(archivo);
    });
  }
}
