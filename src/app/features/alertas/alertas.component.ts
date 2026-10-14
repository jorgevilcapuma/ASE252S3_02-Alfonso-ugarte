import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ResumenAlerta } from '../../core/models/asistencia.model';
import { ApiService } from '../../core/services/api.service';
import { AsistenciaService } from '../../core/services/asistencia.service';
import { NotificacionService } from '../../core/services/notificacion.service';
import { IconoComponent } from '../../shared/components/icono/icono.component';

type Filtro = 'todas' | 'critica' | 'preventiva' | 'hoy';

const POR_PAGINA = 10;

@Component({
  selector: 'app-alertas',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, IconoComponent],
  templateUrl: './alertas.component.html',
  styleUrl: './alertas.component.css'
})
export class AlertasComponent {
  readonly data = inject(AsistenciaService);
  private readonly notificaciones = inject(NotificacionService);

  /** Reglas configuradas en el backend (application.yml → app.alertas). */
  get ventana(): number { return this.data.ventanaAlertas; }
  get umbralCritico(): number { return this.data.faltasCriticas; }
  get umbralPreventivo(): number { return this.data.faltasPreventivas; }

  constructor() {
    // Alertas recalculadas por el backend y faltas de los últimos 7 días lectivos para la tendencia.
    const dias = this.data.diasHabiles(7);
    void this.data.asegurarGestion()
      .then(() => Promise.all([this.data.cargarAlertas(), this.data.cargarAsistencia(dias[0], this.data.hoy)]))
      .catch(error => this.notificaciones.error('No se pudieron cargar las alertas', ApiService.mensaje(error)));
  }

  filtro: Filtro = 'todas';
  busqueda = '';
  dia: string | null = null;
  confirmarMasiva = false;
  pagina = 1;

  /** Calculadas en el backend con las faltas de la ventana de días lectivos. */
  get alertas(): ResumenAlerta[] { return this.data.resumenAlertas(); }
  get criticas(): number { return this.alertas.filter(item => item.nivel === 'critica').length; }
  get preventivas(): number { return this.alertas.filter(item => item.nivel === 'preventiva').length; }
  get faltasHoy(): number { return this.alertas.filter(item => item.faltaHoy).length; }

  get filtradas(): ResumenAlerta[] {
    const texto = this.normalizar(this.busqueda);
    return this.alertas.filter(item =>
      (this.filtro === 'todas' || (this.filtro === 'hoy' ? item.faltaHoy : item.nivel === this.filtro)) &&
      (!this.dia || this.data.obtenerEstado(item.estudiante.id, this.dia) === 'falta') &&
      (!texto || this.normalizar(item.estudiante.nombre).includes(texto) || item.estudiante.dni.includes(texto))
    );
  }

  /** Faltas por día en los últimos 7 días lectivos. */
  get tendencia() {
    const serie = this.data.faltasPorDia(this.data.diasHabiles(7));
    const maximo = Math.max(1, ...serie.map(item => item.faltas));
    const techo = Math.max(5, Math.ceil(maximo / 5) * 5);
    return {
      techo,
      descripcion: 'Faltas por día: ' + serie.map(item => `${item.fecha.slice(8)}: ${item.faltas}`).join(', '),
      guias: [techo, techo / 2, 0],
      barras: serie.map(item => ({ ...item, alto: item.faltas / techo * 100, esMaximo: item.faltas === maximo && maximo > 0, esHoy: item.fecha === this.data.hoy }))
    };
  }

  get totalPaginas(): number { return Math.max(1, Math.ceil(this.filtradas.length / POR_PAGINA)); }
  get paginaActual(): number { return Math.min(this.pagina, this.totalPaginas); }
  get visibles(): ResumenAlerta[] {
    const inicio = (this.paginaActual - 1) * POR_PAGINA;
    return this.filtradas.slice(inicio, inicio + POR_PAGINA);
  }

  /** Cualquier cambio de filtro vuelve a la primera página. */
  filtrar(filtro: Filtro): void {
    this.filtro = this.filtro === filtro && filtro !== 'todas' ? 'todas' : filtro;
    this.pagina = 1;
  }

  elegirDia(fecha: string): void {
    this.dia = this.dia === fecha ? null : fecha;
    this.pagina = 1;
  }

  aula(item: ResumenAlerta): string {
    const { nivel, grado, seccion } = item.estudiante;
    return `${nivel === 'Inicial' ? grado : `${grado}°`} "${seccion}" ${nivel}`;
  }

  async notificar(item: ResumenAlerta): Promise<void> {
    const error = await this.data.registrarAvisoFamilia([item.estudiante.id]);
    if (error) this.notificaciones.error('No se registró el aviso', error);
    else this.notificaciones.exito('Aviso enviado', `Se notificó a la familia de ${item.estudiante.nombre}.`);
  }

  async notificarMasivo(): Promise<void> {
    const destinatarios = this.filtradas.map(item => item.estudiante.id);
    this.confirmarMasiva = false;
    if (destinatarios.length === 0) {
      this.notificaciones.aviso('Sin destinatarios', 'No hay alertas en la lista actual.');
      return;
    }
    const error = await this.data.registrarAvisoFamilia(destinatarios);
    if (error) this.notificaciones.error('No se registraron los avisos', error);
    else this.notificaciones.exito('Notificación masiva enviada', `${destinatarios.length} familias recibieron el aviso de seguimiento.`);
  }

  exportar(): void {
    const filas = this.filtradas.map(item => [
      item.estudiante.nombre, item.estudiante.dni, item.estudiante.nivel, item.estudiante.grado, item.estudiante.seccion,
      String(item.faltas), item.nivel === 'critica' ? 'Crítica' : item.nivel === 'preventiva' ? 'Preventiva' : 'Falta hoy',
      item.faltaHoy ? 'Sí' : 'No', item.ultimoAviso ? new Date(item.ultimoAviso).toLocaleString('es-PE') : 'Sin aviso'
    ]);
    const csv = [['Estudiante', 'DNI', 'Nivel', 'Grado', 'Sección', `Faltas (${this.ventana} días)`, 'Alerta', 'Falta hoy', 'Último aviso'], ...filas]
      .map(fila => fila.map(valor => `"${valor.replaceAll('"', '""')}"`).join(',')).join('\r\n');
    const enlace = document.createElement('a');
    enlace.href = URL.createObjectURL(new Blob(['﻿', csv], { type: 'text/csv;charset=utf-8' }));
    enlace.download = `alertas-asistencia-${this.data.hoy}.csv`;
    enlace.click();
    setTimeout(() => URL.revokeObjectURL(enlace.href), 1000);
    this.notificaciones.exito('Reporte exportado', `${filas.length} alertas en formato CSV.`);
  }

  imprimir(): void { window.print(); }

  iniciales(nombre: string): string { return nombre.split(' ').slice(0, 2).map(parte => parte[0]).join('').toUpperCase(); }

  private normalizar(texto: string): string {
    return texto.trim().normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase();
  }
}
