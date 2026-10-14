import { Injectable, inject, signal } from '@angular/core';
import { ApiService } from './api.service';
import { AuthService, PerfilAcceso } from './auth.service';

export type EstadoUsuario = 'Activo' | 'Inactivo';

export interface UsuarioSistema {
  id: string;
  nombre: string;
  correo: string;
  perfil: PerfilAcceso;
  estado: EstadoUsuario;
  creado: string;
}

interface UsuarioApi { id: number; nombre: string; correo: string; perfil: PerfilAcceso; estado: EstadoUsuario; creado: string; }

/** Cuentas institucionales (tabla usuario del backend; solo el administrador las gestiona). */
@Injectable({ providedIn: 'root' })
export class UsuariosService {
  private readonly api = inject(ApiService);
  readonly usuarios = signal<UsuarioSistema[]>([]);
  private carga: Promise<void> | null = null;

  constructor() {
    inject(AuthService).alCerrarSesion(() => {
      this.usuarios.set([]);
      this.carga = null;
    });
  }

  asegurar(): Promise<void> {
    this.carga ??= this.recargar().catch(error => {
      this.carga = null;
      throw error;
    });
    return this.carga;
  }

  async recargar(): Promise<void> {
    const lista = await this.api.get<UsuarioApi[]>('/usuarios');
    this.usuarios.set(lista.map(u => ({ ...u, id: String(u.id) })));
  }

  /** Crea o actualiza; devuelve la cuenta guardada o el mensaje de error del backend. Sin contraseña se usa la inicial (123456). */
  async guardarUsuario(datos: Omit<UsuarioSistema, 'id' | 'creado'>, id?: string): Promise<UsuarioSistema | string> {
    try {
      const cuerpo = { nombre: datos.nombre, correo: datos.correo, perfil: datos.perfil, estado: datos.estado };
      const guardado = id ? await this.api.put<UsuarioApi>(`/usuarios/${id}`, cuerpo) : await this.api.post<UsuarioApi>('/usuarios', cuerpo);
      await this.recargar();
      return { ...guardado, id: String(guardado.id) };
    } catch (error) {
      return ApiService.mensaje(error);
    }
  }

  async alternarEstado(id: string): Promise<string | null> {
    try {
      await this.api.patch(`/usuarios/${id}/estado`);
      await this.recargar();
      return null;
    } catch (error) {
      return ApiService.mensaje(error);
    }
  }

  async eliminar(id: string): Promise<string | null> {
    try {
      await this.api.delete(`/usuarios/${id}`);
      await this.recargar();
      return null;
    } catch (error) {
      return ApiService.mensaje(error);
    }
  }
}
