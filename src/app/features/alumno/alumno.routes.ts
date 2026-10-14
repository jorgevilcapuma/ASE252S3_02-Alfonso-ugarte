import { Routes } from '@angular/router';
import { MiAsistenciaComponent } from './mi-asistencia/mi-asistencia.component';
import { MisCursosComponent } from './mis-cursos/mis-cursos.component';
import { MiHorarioComponent } from './mi-horario/mi-horario.component';
import { InicioAlumnoComponent } from './inicio-alumno/inicio-alumno.component';
import { MisJustificacionesComponent } from './mis-justificaciones/mis-justificaciones.component';
import { MisAlertasComponent } from './mis-alertas/mis-alertas.component';
import { ComunicacionesComponent } from './comunicaciones/comunicaciones.component';
import { MiPerfilComponent } from './mi-perfil/mi-perfil.component';

export const alumnoRoutes: Routes = [
  { path: 'inicio', component: InicioAlumnoComponent },
  { path: 'asistencia', component: MiAsistenciaComponent },
  { path: 'horario', component: MiHorarioComponent },
  { path: 'cursos', component: MisCursosComponent },
  { path: 'justificaciones', component: MisJustificacionesComponent },
  { path: 'alertas', component: MisAlertasComponent },
  { path: 'comunicaciones', component: ComunicacionesComponent },
  { path: 'perfil', component: MiPerfilComponent },
  { path: '', pathMatch: 'full', redirectTo: 'inicio' }
];
