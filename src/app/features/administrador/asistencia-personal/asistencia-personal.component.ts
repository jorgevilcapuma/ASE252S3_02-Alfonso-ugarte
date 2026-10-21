import { DatePipe } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../../core/services/api.service';
import { AsistenciaService } from '../../../core/services/asistencia.service';
import { NotificacionService } from '../../../core/services/notificacion.service';
import { IconoComponent } from '../../../shared/components/icono/icono.component';

type EstadoPersonal = 'puntual' | 'tardanza' | 'sin-marcar';

interface FilaPersonal {
  personalId: number; nombreCompleto: string; dni: string; cargo: string; area?: string;
  horaEntrada?: string; horaSalida?: string; estado: EstadoPersonal; minutosTarde: number; observacion?: string;
}
interface DiaPersonal {
  fecha: string; horaEntrada: string; minutosTolerancia: number; limitePuntualidad: string;
  resumen: { total: number; puntuales: number; tardanzas: number; sinMarcar: number; conSalida: number };
  filas: FilaPersonal[];
}
interface FilaReporte {
  personalId: number; nombreCompleto: string; cargo: string; diasHabiles: number; diasAsistidos: number; tardanzas: number;
  minutosTardeAcumulados: number; diasSinMarcar: number; porcentajeAsistencia: number;
}
interface ReportePersonal { mes: string; diasHabiles: number; filas: FilaReporte[]; }

/** Entrada y salida del personal (docentes y administrativos), separada de la asistencia de los estudiantes. */
@Component({
  selector: 'app-asistencia-personal',
  standalone: true,
  imports: [DatePipe, FormsModule, IconoComponent],
  templateUrl: './asistencia-personal.component.html',
  styleUrl: './asistencia-personal.component.css'
})
export class AsistenciaPersonalComponent {
  private readonly api = inject(ApiService);
  private readonly data = inject(AsistenciaService);
  private readonly notificaciones = inject(NotificacionService);

  readonly hoy = this.data.hoy;
  vista: 'dia' | 'reporte' = 'dia';
  fecha = this.hoy;
  mes = this.hoy.slice(0, 7);
  busqueda = '';
  cargando = true;
  dia: DiaPersonal | null = null;
  reporte: ReportePersonal | null = null;

  /** Corrección manual de una marcación (días anteriores u horas mal registradas). */
  edicion: { fila: FilaPersonal; entrada: string; salida: string; observacion: string } | null = null;
  ocupado = false;

  constructor() { void this.cargarDia(); }

  get esHoy(): boolean { return this.fecha === this.hoy; }

  /** Sábado o domingo: no hay jornada que marcar. */
  get esFinDeSemana(): boolean { return [0, 6].includes(new Date(`${this.fecha}T12:00:00`).getDay()); }

  get marcable(): boolean { return this.esHoy && !this.esFinDeSemana; }

  get filas(): FilaPersonal[] {
    const termino = this.normalizar(this.busqueda);
    return (this.dia?.filas ?? []).filter(f => !termino || this.normalizar(`${f.nombreCompleto} ${f.dni} ${f.cargo} ${f.area ?? ''}`).includes(termino));
  }

  get filasReporte(): FilaReporte[] {
    const termino = this.normalizar(this.busqueda);
    return (this.reporte?.filas ?? []).filter(f => !termino || this.normalizar(`${f.nombreCompleto} ${f.cargo}`).includes(termino));
  }

  cambiarVista(vista: 'dia' | 'reporte'): void {
    this.vista = vista;
    void (vista === 'dia' ? this.cargarDia() : this.cargarReporte());
  }

  async cargarDia(): Promise<void> {
    if (!this.fecha || this.fecha > this.hoy) this.fecha = this.hoy;
    this.cargando = true;
    try {
      this.dia = await this.api.get<DiaPersonal>('/asistencia-personal', { fecha: this.fecha });
    } catch (error) {
      this.notificaciones.error('No se pudo cargar la asistencia del personal', ApiService.mensaje(error));
    }
    this.cargando = false;
  }

  async cargarReporte(): Promise<void> {
    if (!this.mes) this.mes = this.hoy.slice(0, 7);
    this.cargando = true;
    try {
      this.reporte = await this.api.get<ReportePersonal>('/asistencia-personal/reporte', { mes: this.mes });
    } catch (error) {
      this.notificaciones.error('No se pudo cargar el reporte', ApiService.mensaje(error));
    }
    this.cargando = false;
  }

  /** Marca con la hora actual (solo para hoy). */
  async marcar(fila: FilaPersonal, tipo: 'entrada' | 'salida'): Promise<void> {
    if (this.ocupado) return;
    this.ocupado = true;
    try {
      const actualizada = await this.api.put<FilaPersonal>(`/asistencia-personal/${fila.personalId}/${tipo}`, {});
      this.reemplazar(actualizada);
      this.notificaciones.exito(tipo === 'entrada' ? 'Entrada registrada' : 'Salida registrada',
        `${actualizada.nombreCompleto} · ${tipo === 'entrada' ? actualizada.horaEntrada : actualizada.horaSalida}${actualizada.estado === 'tardanza' && tipo === 'entrada' ? ` (tardanza de ${actualizada.minutosTarde} min)` : ''}`);
    } catch (error) {
      this.notificaciones.error('No se pudo registrar', ApiService.mensaje(error));
    }
    this.ocupado = false;
  }

  editar(fila: FilaPersonal): void {
    this.edicion = { fila, entrada: fila.horaEntrada ?? '', salida: fila.horaSalida ?? '', observacion: fila.observacion ?? '' };
  }

  async guardarEdicion(): Promise<void> {
    const e = this.edicion;
    if (!e || this.ocupado) return;
    if (!e.entrada) {
      this.notificaciones.error('Falta la hora de entrada', 'Indica la hora de entrada para guardar la marcación.');
      return;
    }
    if (e.salida && e.salida <= e.entrada) {
      this.notificaciones.error('Hora de salida no válida', `La salida debe ser posterior a la entrada (${e.entrada}).`);
      return;
    }
    if (this.fecha === this.hoy && e.entrada > this.horaActual()) {
      this.notificaciones.error('Hora no válida', 'No se puede registrar una hora futura.');
      return;
    }
    this.ocupado = true;
    try {
      const observacion = e.observacion.trim() || undefined;
      let fila = await this.api.put<FilaPersonal>(`/asistencia-personal/${e.fila.personalId}/entrada`, { fecha: this.fecha, hora: e.entrada, observacion });
      if (e.salida) fila = await this.api.put<FilaPersonal>(`/asistencia-personal/${e.fila.personalId}/salida`, { fecha: this.fecha, hora: e.salida });
      this.edicion = null;
      await this.cargarDia();
      this.notificaciones.exito('Marcación guardada', `${fila.nombreCompleto} · ${fila.horaEntrada}${fila.horaSalida ? ` – ${fila.horaSalida}` : ''}`);
    } catch (error) {
      this.notificaciones.error('No se pudo guardar', ApiService.mensaje(error));
    }
    this.ocupado = false;
  }

  async anular(fila: FilaPersonal): Promise<void> {
    if (this.ocupado || !confirm(`¿Anular la marcación de ${fila.nombreCompleto} del ${this.fecha}?`)) return;
    this.ocupado = true;
    try {
      await this.api.delete(`/asistencia-personal/${fila.personalId}/${this.fecha}`);
      await this.cargarDia();
      this.notificaciones.exito('Marcación anulada', fila.nombreCompleto);
    } catch (error) {
      this.notificaciones.error('No se pudo anular', ApiService.mensaje(error));
    }
    this.ocupado = false;
  }

  exportar(): void {
    if (this.vista === 'dia') {
      this.descargarCsv(['Personal', 'DNI', 'Cargo', 'Área', 'Entrada', 'Salida', 'Estado', 'Minutos tarde', 'Observación'],
        this.filas.map(f => [f.nombreCompleto, f.dni, f.cargo, f.area ?? '', f.horaEntrada ?? '', f.horaSalida ?? '', this.etiqueta(f.estado),
          String(f.minutosTarde), f.observacion ?? '']), `asistencia-personal-${this.fecha}.csv`);
    } else {
      this.descargarCsv(['Personal', 'Cargo', 'Días hábiles', 'Días asistidos', 'Asistencia %', 'Tardanzas', 'Minutos tarde', 'Días sin marcar'],
        this.filasReporte.map(f => [f.nombreCompleto, f.cargo, String(f.diasHabiles), String(f.diasAsistidos), String(f.porcentajeAsistencia),
          String(f.tardanzas), String(f.minutosTardeAcumulados), String(f.diasSinMarcar)]), `reporte-personal-${this.mes}.csv`);
    }
  }

  private horaActual(): string {
    const ahora = new Date();
    return `${String(ahora.getHours()).padStart(2, '0')}:${String(ahora.getMinutes()).padStart(2, '0')}`;
  }

  etiqueta(estado: EstadoPersonal): string {
    return estado === 'puntual' ? 'Puntual' : estado === 'tardanza' ? 'Tardanza' : 'Sin marcar';
  }

  iniciales(nombre: string): string {
    const [apellidos, nombres] = nombre.split(',').map(parte => parte.trim());
    return `${nombres?.[0] ?? ''}${apellidos?.[0] ?? ''}`.toUpperCase();
  }

  private reemplazar(fila: FilaPersonal): void {
    if (!this.dia) return;
    this.dia.filas = this.dia.filas.map(f => f.personalId === fila.personalId ? fila : f);
    const filas = this.dia.filas;
    const puntuales = filas.filter(f => f.estado === 'puntual').length;
    const tardanzas = filas.filter(f => f.estado === 'tardanza').length;
    this.dia.resumen = { total: filas.length, puntuales, tardanzas, sinMarcar: filas.length - puntuales - tardanzas, conSalida: filas.filter(f => f.horaSalida).length };
  }

  private descargarCsv(encabezado: string[], filas: string[][], nombre: string): void {
    const csv = [encabezado, ...filas].map(fila => fila.map(valor => `"${valor.replaceAll('"', '""')}"`).join(',')).join('\r\n');
    const enlace = document.createElement('a');
    enlace.href = URL.createObjectURL(new Blob(['﻿', csv], { type: 'text/csv;charset=utf-8' }));
    enlace.download = nombre;
    enlace.click();
    URL.revokeObjectURL(enlace.href);
  }

  private normalizar(texto: string): string {
    return texto.trim().normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase();
  }
}
