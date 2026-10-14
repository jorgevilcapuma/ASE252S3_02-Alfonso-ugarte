import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { EstadoAsistencia, Estudiante, NivelEducativo } from '../../../core/models/asistencia.model';
import { ApiService } from '../../../core/services/api.service';
import { AsistenciaService } from '../../../core/services/asistencia.service';
import { NotificacionService } from '../../../core/services/notificacion.service';
import { IconoComponent } from '../../../shared/components/icono/icono.component';

type Vista = 'diaria' | 'mensual';

interface DiaColumna { fecha: string; numero: string; semana: string; noLectivo: boolean; futuro: boolean; hoy: boolean; }

const LETRA: Record<EstadoAsistencia, string> = { presente: 'P', tardanza: 'T', falta: 'F', justificado: 'J', pendiente: '·' };
const CICLO: EstadoAsistencia[] = ['presente', 'tardanza', 'falta', 'justificado', 'pendiente'];
const DIAS_SEMANA = ['DO', 'LU', 'MA', 'MI', 'JU', 'VI', 'SA'];

@Component({
  selector: 'app-matriz-asistencia',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, IconoComponent],
  templateUrl: './matriz-asistencia.component.html',
  styleUrl: './matriz-asistencia.component.css'
})
export class MatrizAsistenciaComponent {
  readonly data = inject(AsistenciaService);
  private readonly notificaciones = inject(NotificacionService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly estadosRegistro: EstadoAsistencia[] = ['presente', 'tardanza', 'falta', 'justificado'];
  readonly letra = LETRA;

  vista: Vista = 'diaria';
  nivel: NivelEducativo = 'Primaria';
  grado = '1';
  seccion = 'A';
  fecha = this.data.hoy;
  mes = this.data.hoy.slice(0, 7);
  busqueda = '';

  estudianteId = '';
  motivo = '';
  mostrarFormulario = false;

  constructor() {
    this.route.queryParamMap.pipe(takeUntilDestroyed()).subscribe(params => {
      const nivel = params.get('nivel') as NivelEducativo | null;
      if (nivel && this.data.niveles.includes(nivel)) this.nivel = nivel;
      const grado = params.get('grado');
      if (grado && this.grados.includes(grado)) this.grado = grado;
      else if (!this.grados.includes(this.grado)) this.grado = this.grados[0];
      const seccion = params.get('seccion');
      if (seccion && this.data.secciones.includes(seccion)) this.seccion = seccion;
      const vista = params.get('vista');
      if (vista === 'mensual' || vista === 'diaria') this.vista = vista;
      void this.cargar();
    });
  }

  /**
   * Trae del backend la asistencia del aula visible: el día elegido o, en la vista mensual,
   * el mes elegido y el anterior (para la variación del promedio).
   */
  async cargar(): Promise<void> {
    await this.data.asegurarGestion();
    const seccionId = this.data.seccionId(this.nivel, this.grado, this.seccion);
    if (!seccionId) return;
    const [desde, hasta] = this.vista === 'mensual'
      ? [`${this.mesAnterior(this.mes)}-01`, this.diasDelMesFinal(this.mes)]
      : [this.fecha, this.fecha];
    try {
      await this.data.cargarAsistencia(desde, hasta, seccionId);
    } catch (error) {
      this.notificaciones.error('No se pudo cargar la asistencia', ApiService.mensaje(error));
    }
  }

  private diasDelMesFinal(mes: string): string {
    const dias = this.data.diasDelMes(mes);
    return dias[dias.length - 1];
  }

  get grados(): string[] { return this.data.gradosPorNivel[this.nivel]; }
  get estudiantes(): Estudiante[] { return this.data.estudiantesDeAula(this.nivel, this.grado, this.seccion); }
  get filtrados(): Estudiante[] {
    const texto = this.normalizar(this.busqueda);
    return texto ? this.estudiantes.filter(item => this.normalizar(item.nombre).includes(texto) || item.dni.includes(texto)) : this.estudiantes;
  }
  get etiquetaAula(): string { return `${this.data.etiquetaGrado(this.nivel, this.grado)} "${this.seccion}" · ${this.nivel}`; }
  get esFuturo(): boolean { return this.fecha > this.data.hoy; }

  contar(estado: EstadoAsistencia): number { return this.estudiantes.filter(item => this.estado(item.id) === estado).length; }

  /* ---------- Vista mensual ---------- */
  get columnas(): DiaColumna[] {
    return this.data.diasDelMes(this.mes).map(fecha => {
      const dia = new Date(`${fecha}T12:00:00`).getDay();
      return { fecha, numero: fecha.slice(8), semana: DIAS_SEMANA[dia], noLectivo: dia === 0 || dia === 6, futuro: fecha > this.data.hoy, hoy: fecha === this.data.hoy };
    });
  }

  private get diasLectivos(): string[] { return this.columnas.filter(dia => !dia.noLectivo && !dia.futuro).map(dia => dia.fecha); }

  porcentajeEstudiante(id: string, fechas = this.diasLectivos): number | null {
    const registrados = fechas.map(fecha => this.data.obtenerEstado(id, fecha)).filter(estado => estado !== 'pendiente');
    if (registrados.length === 0) return null;
    return Math.round(registrados.filter(estado => estado !== 'falta').length / registrados.length * 1000) / 10;
  }

  /** Promedio del aula en el mes; incluye la variación respecto del mes anterior. */
  get kpis() {
    const fechas = this.diasLectivos;
    const estados = this.estudiantes.flatMap(item => fechas.map(fecha => this.data.obtenerEstado(item.id, fecha))).filter(estado => estado !== 'pendiente');
    const faltas = estados.filter(estado => estado === 'falta').length;
    const justificadas = estados.filter(estado => estado === 'justificado').length;
    const tardanzas = estados.filter(estado => estado === 'tardanza').length;
    const promedio = estados.length ? Math.round((estados.length - faltas) / estados.length * 1000) / 10 : 0;

    const anterior = this.mesAnterior(this.mes);
    const fechasAnteriores = this.data.diasDelMes(anterior).filter(fecha => !this.data.esFinDeSemana(fecha) && fecha <= this.data.hoy);
    const previos = this.estudiantes.flatMap(item => fechasAnteriores.map(fecha => this.data.obtenerEstado(item.id, fecha))).filter(estado => estado !== 'pendiente');
    const promedioAnterior = previos.length ? Math.round((previos.length - previos.filter(estado => estado === 'falta').length) / previos.length * 1000) / 10 : null;

    return {
      promedio,
      variacion: promedioAnterior === null ? null : Math.round((promedio - promedioAnterior) * 10) / 10,
      faltas,
      justificadas,
      inasistencias: faltas + justificadas,
      tardanzas,
      dias: fechas.length
    };
  }

  /** Clic en una celda del mes: recorre P → T → F → J → sin registro. */
  alternarCelda(estudiante: Estudiante, dia: DiaColumna): void {
    if (dia.noLectivo || dia.futuro) return;
    const actual = this.data.obtenerEstado(estudiante.id, dia.fecha);
    const siguiente = CICLO[(CICLO.indexOf(actual) + 1) % CICLO.length];
    void this.data.guardarEstado(estudiante.id, dia.fecha, siguiente);
  }

  /* ---------- Acciones ---------- */
  cambiarNivel(): void {
    this.grado = this.grados[0];
    this.sincronizarUrl();
  }

  sincronizarUrl(): void {
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { nivel: this.nivel, grado: this.grado, seccion: this.seccion, vista: this.vista },
      replaceUrl: true
    });
  }

  cambiarVista(vista: Vista): void {
    this.vista = vista;
    if (vista === 'mensual') this.mes = this.fecha.slice(0, 7);
    this.sincronizarUrl();
    void this.cargar();
  }

  estado(id: string): EstadoAsistencia { return this.data.obtenerEstado(id, this.fecha); }

  actualizar(id: string, estado: EstadoAsistencia): void {
    if (this.esFuturo) {
      this.notificaciones.error('Fecha no válida', 'No se puede registrar asistencia de una fecha futura.');
      return;
    }
    // Volver a pulsar el estado activo lo deja sin registrar.
    void this.data.guardarEstado(id, this.fecha, this.estado(id) === estado ? 'pendiente' : estado);
  }

  async marcarTodos(): Promise<void> {
    if (this.esFuturo) {
      this.notificaciones.error('Fecha no válida', 'No se puede registrar asistencia de una fecha futura.');
      return;
    }
    const pendientes = this.estudiantes.filter(item => this.estado(item.id) === 'pendiente');
    const seccionId = this.data.seccionId(this.nivel, this.grado, this.seccion);
    if (pendientes.length && seccionId) {
      const error = await this.data.guardarEstados(seccionId, this.fecha, pendientes.map(item => ({ estudianteId: item.id, estado: 'presente' as const })));
      if (error) {
        this.notificaciones.error('No se guardó la asistencia', error);
        return;
      }
    }
    this.notificaciones.exito('Asistencia registrada', pendientes.length
      ? `${pendientes.length} estudiantes sin registro quedaron como presentes.`
      : 'Todos los estudiantes ya tenían un estado registrado.');
  }

  abrirJustificacion(estudianteId = this.estudiantes[0]?.id ?? ''): void {
    this.estudianteId = estudianteId;
    this.motivo = '';
    this.mostrarFormulario = true;
  }

  async guardarJustificacion(): Promise<void> {
    const error = await this.data.registrarJustificacion(this.estudianteId, this.fecha, this.motivo);
    if (error) {
      this.notificaciones.error('No se registró la justificación', error);
      return;
    }
    this.mostrarFormulario = false;
    this.notificaciones.exito('Justificación registrada', 'Quedó pendiente de revisión en Justificaciones.');
  }

  exportar(): void {
    if (this.vista === 'mensual') {
      const columnas = this.columnas.filter(dia => !dia.noLectivo);
      const filas = this.estudiantes.map((item, indice) => [
        String(indice + 1), item.nombre, item.dni,
        ...columnas.map(dia => dia.futuro ? '' : this.letra[this.data.obtenerEstado(item.id, dia.fecha)]),
        this.porcentajeEstudiante(item.id)?.toString() ?? ''
      ]);
      this.descargarCsv(['N°', 'Estudiante', 'DNI', ...columnas.map(dia => `${dia.numero} ${dia.semana}`), 'Asistencia %'], filas,
        `asistencia-${this.nivel.toLowerCase()}-${this.grado}-${this.seccion}-${this.mes}.csv`.replace(/\s+/g, ''));
    } else {
      const filas = this.estudiantes.map((item, indice) => [
        String(indice + 1), item.nombre, item.dni, item.nivel, item.grado, item.seccion, this.fecha, this.etiquetaEstado(this.estado(item.id))
      ]);
      this.descargarCsv(['N°', 'Estudiante', 'DNI', 'Nivel', 'Grado', 'Sección', 'Fecha', 'Estado'], filas,
        `asistencia-${this.nivel.toLowerCase()}-${this.grado}-${this.seccion}-${this.fecha}.csv`.replace(/\s+/g, ''));
    }
    this.notificaciones.exito('Archivo exportado', 'Se descargó el registro en formato CSV.');
  }

  iniciales(nombre: string): string { return nombre.split(' ').slice(0, 2).map(parte => parte[0]).join('').toUpperCase(); }
  etiquetaEstado(estado: EstadoAsistencia): string { return ({ presente: 'Presente', tardanza: 'Tardanza', falta: 'Falta', justificado: 'Justificado', pendiente: 'Sin registrar' })[estado]; }

  private mesAnterior(mes: string): string {
    const [anio, numero] = mes.split('-').map(Number);
    const fecha = new Date(anio, numero - 2, 1);
    return `${fecha.getFullYear()}-${String(fecha.getMonth() + 1).padStart(2, '0')}`;
  }

  private normalizar(texto: string): string {
    return texto.trim().normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase();
  }

  private descargarCsv(encabezado: string[], filas: string[][], nombre: string): void {
    const contenido = [encabezado, ...filas].map(fila => fila.map(valor => `"${valor.replaceAll('"', '""')}"`).join(',')).join('\r\n');
    const enlace = document.createElement('a');
    enlace.href = URL.createObjectURL(new Blob(['﻿', contenido], { type: 'text/csv;charset=utf-8' }));
    enlace.download = nombre;
    enlace.click();
    setTimeout(() => URL.revokeObjectURL(enlace.href), 1000);
  }
}
