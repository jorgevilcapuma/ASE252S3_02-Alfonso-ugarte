import { Routes } from '@angular/router';

import { AlertasComponent } from '../alertas/alertas.component';
import { JustificacionesComponent } from '../justificaciones/justificaciones.component';
import { MatrizAsistenciaComponent } from '../asistencia/matriz-asistencia/matriz-asistencia.component';
import { SeleccionSeccionComponent } from '../asistencia/seleccion-seccion/seleccion-seccion.component';
import { ComunicadosComponent } from './comunicados/comunicados.component';
import { InicioDocenteComponent } from './inicio-docente/inicio-docente.component';
import { RegistrarAsistenciaComponent } from './registrar-asistencia/registrar-asistencia.component';
import { HistorialAsistenciaComponent } from './historial-asistencia/historial-asistencia.component';
import { ReportesComponent } from './reportes/reportes.component';
import { MisCursosComponent } from './mis-cursos/mis-cursos.component';
import { MisAlumnosComponent } from './mis-alumnos/mis-alumnos.component';
import { MiPerfilComponent } from './mi-perfil/mi-perfil.component';

export const docenteRoutes: Routes = [
  { path: 'dashboard', component: InicioDocenteComponent },
  { path: 'registrar-asistencia', component: RegistrarAsistenciaComponent },
  { path: 'historial', component: HistorialAsistenciaComponent },
  { path: 'reportes', component: ReportesComponent },
  { path: 'secciones', component: SeleccionSeccionComponent },
  { path: 'asistencia', component: MatrizAsistenciaComponent },
  { path: 'justificaciones', component: JustificacionesComponent },
  { path: 'alertas', component: AlertasComponent },
  { path: 'comunicados', component: ComunicadosComponent },
  { path: 'perfil', component: MiPerfilComponent },
  { path: 'cursos', component: MisCursosComponent },
  { path: 'estudiantes', component: MisAlumnosComponent },
  { path: '', pathMatch: 'full', redirectTo: 'dashboard' }
];