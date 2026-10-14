import { Routes } from '@angular/router';
import { loginGuard, perfilGuard } from './core/guards/perfil.guard';
import { LoginComponent } from './features/auth/login/login.component';

/** Cada perfil carga su propio módulo de rutas (lazy) para poder crecer sin mezclar pantallas. */
const perfil = (path: string, carga: () => ReturnType<NonNullable<Routes[number]['loadChildren']>>) => ({
  path,
  canActivate: [perfilGuard],
  canActivateChild: [perfilGuard],
  loadChildren: carga
});

export const routes: Routes = [
  { path: 'login', component: LoginComponent, canActivate: [loginGuard] },
  perfil('administrador', () => import('./features/administrador/administrador.routes').then(m => m.administradorRoutes)),
  perfil('docente', () => import('./features/docente/docente.routes').then(m => m.docenteRoutes)),
  perfil('alumno', () => import('./features/alumno/alumno.routes').then(m => m.alumnoRoutes)),

  /* URLs antiguas (sin perfil): se conservan solo como redirección para enlaces guardados. */
  { path: 'dashboard', redirectTo: 'administrador/dashboard', pathMatch: 'full' },
  { path: 'asistencia', redirectTo: 'administrador/asistencia', pathMatch: 'full' },
  { path: 'justificaciones', redirectTo: 'administrador/justificaciones', pathMatch: 'full' },
  { path: 'alertas', redirectTo: 'administrador/alertas', pathMatch: 'full' },
  { path: 'configuracion', redirectTo: 'administrador/configuracion', pathMatch: 'full' },
  { path: 'fases', redirectTo: 'administrador/fases', pathMatch: 'full' },
  { path: 'mi-asistencia', redirectTo: 'alumno/asistencia', pathMatch: 'full' },

  { path: '', pathMatch: 'full', redirectTo: 'login' },
  { path: '**', redirectTo: 'login' }
];
