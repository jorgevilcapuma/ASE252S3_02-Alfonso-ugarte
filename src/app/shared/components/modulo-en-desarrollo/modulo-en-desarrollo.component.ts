import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { IconoComponent } from '../icono/icono.component';
import { ActivatedRoute } from '@angular/router';

interface FuncionalidadPendiente {
  nombre: string;
  descripcion: string;
}

@Component({
  selector: 'app-modulo-en-desarrollo',
  standalone: true,
  imports: [CommonModule, IconoComponent],
  templateUrl: './modulo-en-desarrollo.component.html',
  styleUrl: './modulo-en-desarrollo.component.css'
})
export class ModuloEnDesarrolloComponent {
  private readonly route = inject(ActivatedRoute);

  get titulo(): string { return this.route.snapshot.data['titulo'] as string ?? 'Módulo'; }
  get descripcion(): string { return this.route.snapshot.data['descripcion'] as string ?? ''; }
  get rol(): string { return this.route.snapshot.data['rol'] as string ?? ''; }
  get funcionalidades(): FuncionalidadPendiente[] {
    return this.route.snapshot.data['funcionalidades'] as FuncionalidadPendiente[] ?? [];
  }
}
