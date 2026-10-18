import { Routes } from '@angular/router';
import { AlertasComponent } from '../alertas/alertas.component';
import { JustificacionesComponent } from '../justificaciones/justificaciones.component';
import { MatrizAsistenciaComponent } from '../asistencia/matriz-asistencia/matriz-asistencia.component';
import { SeleccionSeccionComponent } from '../asistencia/seleccion-seccion/seleccion-seccion.component';
import { ConfiguracionComponent } from '../configuracion/configuracion.component';
import { DashboardComponent } from '../dashboard/dashboard.component';
import { FasesComponent } from '../proyecto/fases/fases.component';
import { AsistenciaPersonalComponent } from './asistencia-personal/asistencia-personal.component';
import { GestionComponent } from './gestion/gestion.component';
import { UsuariosComponent } from './usuarios/usuarios.component';

export const administradorRoutes: Routes = [
  { path: 'dashboard', component: DashboardComponent },
  { path: 'secciones', component: SeleccionSeccionComponent },
  { path: 'asistencia', component: MatrizAsistenciaComponent },
  { path: 'justificaciones', component: JustificacionesComponent },
  { path: 'alertas', component: AlertasComponent },
  { path: 'fases', component: FasesComponent },
  { path: 'configuracion', component: ConfiguracionComponent },
  { path: 'usuarios', component: UsuariosComponent },
  { path: 'asistencia-personal', component: AsistenciaPersonalComponent },
  { path: 'docentes', component: GestionComponent, data: { entidad: 'docentes' } },
  { path: 'estudiantes', component: GestionComponent, data: { entidad: 'estudiantes' } },
  { path: 'cursos', component: GestionComponent, data: { entidad: 'cursos' } },
  { path: 'grados-secciones', component: GestionComponent, data: { entidad: 'grados-secciones' } },
  { path: '', pathMatch: 'full', redirectTo: 'dashboard' }
];
