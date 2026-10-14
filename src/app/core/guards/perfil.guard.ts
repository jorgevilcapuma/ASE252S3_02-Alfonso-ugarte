import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

/** Exige sesión y que la ruta pertenezca al perfil (administrador, docente o alumno). */
export const perfilGuard: CanActivateFn = (_route, state) => {
  const router = inject(Router);
  const auth = inject(AuthService);
  if (!auth.isAuthenticated) return router.createUrlTree(['/login']);
  return auth.puedeAcceder(state.url.split('?')[0]) || router.createUrlTree([auth.rutaInicial()]);
};

/** Si ya hay sesión, el login redirige al inicio del perfil. */
export const loginGuard: CanActivateFn = () => {
  const router = inject(Router);
  const auth = inject(AuthService);
  return auth.isAuthenticated ? router.createUrlTree([auth.rutaInicial()]) : true;
};
