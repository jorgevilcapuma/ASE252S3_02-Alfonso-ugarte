import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ETIQUETA_PERFIL } from '../../../core/config/navegacion';
import { ApiService } from '../../../core/services/api.service';
import { AsistenciaService } from '../../../core/services/asistencia.service';
import { AuthService, DatosIngreso, IngresoValido, PerfilAcceso } from '../../../core/services/auth.service';
import { ModoDatos, ModoDatosService } from '../../../core/services/modo-datos.service';
import { NotificacionService } from '../../../core/services/notificacion.service';
import { TemaService } from '../../../core/services/tema.service';
import { IconoComponent } from '../../../shared/components/icono/icono.component';
import { DURACION_AUTENTICACION, PantallaCargaComponent } from '../../../shared/components/pantalla-carga/pantalla-carga.component';

/** Cuentas de demostración que existen en la base de datos (contraseña 123456). */
const DEMO: Record<PerfilAcceso, { correo?: string; contrasena?: string; nombre?: string; dni?: string }> = {
  administrador: { correo: 'admin@ie20191.edu.pe', contrasena: '123456' },
  docente: { correo: 'elena.salazar@ie20191.edu.pe', contrasena: '123456' },
  alumno: { nombre: 'Juan Pérez Ugarte', dni: '72819201' }
};

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule, IconoComponent, PantallaCargaComponent],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css'
})
export class LoginComponent {
  private readonly auth = inject(AuthService);
  private readonly data = inject(AsistenciaService);
  private readonly router = inject(Router);
  private readonly notificaciones = inject(NotificacionService);
  readonly tema = inject(TemaService);
  readonly modo = inject(ModoDatosService);
  private readonly api = inject(ApiService);

  readonly etiquetas = ETIQUETA_PERFIL;
  readonly perfiles: PerfilAcceso[] = ['administrador', 'docente', 'alumno'];
  readonly anio = new Date().getFullYear();

  nombre = '';
  dni = '';
  correo = '';
  contrasena = '';
  perfil: PerfilAcceso | '' = '';
  mostrarContrasena = false;
  mensaje = '';
  readonly autenticando = signal(false);
  private validando = false;
  /** Nombre que se saluda en la pantalla "Autenticando sesión". */
  nombreIngreso = '';

  get nombreCorto(): string { return this.data.nombreInstitucion.replace(/\s*Alfonso Ugarte\s*/i, '').trim() || this.data.nombreInstitucion; }

  /** Al cambiar de perfil se completan los datos de una cuenta de demostración. */
  onPerfilChange(): void {
    this.mensaje = '';
    if (!this.perfil) return;
    const demo = DEMO[this.perfil];
    if (this.perfil === 'alumno') {
      this.nombre = demo.nombre ?? '';
      this.dni = demo.dni ?? '';
    } else {
      this.correo = demo.correo ?? '';
      this.contrasena = demo.contrasena ?? '';
    }
  }

  soloDigitos(): void {
    this.dni = this.dni.replace(/\D/g, '').slice(0, 8);
  }

  async ingresar(): Promise<void> {
    if (this.autenticando() || this.validando) return;
    this.mensaje = '';
    if (!this.perfil) {
      this.mensaje = 'Selecciona tu perfil de acceso.';
      return;
    }

    let datos: DatosIngreso;
    if (this.perfil === 'alumno') {
      const nombre = this.nombre.trim().replace(/\s+/g, ' ');
      if (nombre.split(' ').length < 2) {
        this.mensaje = 'Ingresa tu nombre completo con al menos un apellido.';
        return;
      }
      if (!/^\d{8}$/.test(this.dni)) {
        this.mensaje = 'El DNI debe tener exactamente 8 dígitos.';
        return;
      }
      datos = { perfil: 'alumno', nombre, dni: this.dni };
    } else {
      const correo = this.correo.trim().toLowerCase();
      if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(correo)) {
        this.mensaje = 'Ingresa un correo institucional válido.';
        return;
      }
      if (this.contrasena.length < 6) {
        this.mensaje = 'La contraseña debe tener 6 caracteres como mínimo.';
        return;
      }
      datos = { perfil: this.perfil, correo, contrasena: this.contrasena };
    }

    // El backend valida la cuenta (estado, perfil y contraseña) antes de la animación de ingreso.
    let ingreso: IngresoValido;
    this.validando = true;
    try {
      ingreso = await this.auth.autenticar(datos);
    } catch (error) {
      this.mensaje = ApiService.mensaje(error);
      return;
    } finally {
      this.validando = false;
    }

    // La sesión se abre recién al terminar la animación: si se abriera antes, la app
    // mostraría el portal con la ruta /login todavía activa (formulario dentro del menú).
    this.nombreIngreso = ingreso.sesion.nombre.split(' ')[0];
    this.autenticando.set(true);
    setTimeout(() => {
      this.auth.abrirSesion(ingreso);
      void this.router.navigateByUrl(this.auth.rutaInicial(), { replaceUrl: true });
      this.notificaciones.exito('¡Bienvenido!', `Ingresaste como ${ingreso.sesion.nombre}.`);
    }, DURACION_AUTENTICACION);
  }

  /** Cambia entre el backend y el modo local; si el servidor no responde, lo avisa. */
  async cambiarModo(modo: ModoDatos): Promise<void> {
    if (modo === this.modo.modo() && !this.modo.automatico()) return;
    this.mensaje = '';
    if (modo === 'servidor' && !(await this.modo.servidorDisponible())) {
      this.mensaje = 'El servidor no responde en http://localhost:8080. Inícialo o sigue en el modo local.';
      return;
    }
    this.modo.elegir(modo);
    this.auth.signOut();
    await this.data.cargarInstitucion();
    this.notificaciones.info(modo === 'local' ? 'Modo local' : 'Modo servidor',
      modo === 'local' ? 'Los datos se guardan en este navegador.' : 'Los datos se leen y guardan en el backend.');
  }

  reiniciarLocal(): void {
    this.api.reiniciarDatosLocales();
    this.auth.signOut();
    void this.data.cargarInstitucion();
    this.notificaciones.info('Datos reiniciados', 'El modo local volvió a los datos de demostración.');
  }

  olvideContrasena(): void {
    this.notificaciones.info('Recuperar contraseña', 'Solicita el restablecimiento al administrador del portal (admin@ie20191.edu.pe).');
  }

  solicitarRegistro(): void {
    this.notificaciones.info('Registro institucional', 'Las cuentas las crea el administrador desde Gestión de usuarios.');
  }
}
