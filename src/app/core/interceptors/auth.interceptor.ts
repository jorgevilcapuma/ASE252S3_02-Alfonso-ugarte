import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { API_URL } from '../config/api';
import { AuthService } from '../services/auth.service';

/** Envía el token de la sesión al backend y, si expiró (401), vuelve al login. */
export const authInterceptor: HttpInterceptorFn = (peticion, siguiente) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const token = auth.token;
  const conToken = token && peticion.url.startsWith(API_URL)
    ? peticion.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : peticion;

  return siguiente(conToken).pipe(
    catchError((error: unknown) => {
      const esLogin = peticion.url.endsWith('/auth/login');
      if (error instanceof HttpErrorResponse && error.status === 401 && !esLogin && auth.isAuthenticated) {
        auth.signOut();
        void router.navigateByUrl('/login');
      }
      return throwError(() => error);
    })
  );
};
