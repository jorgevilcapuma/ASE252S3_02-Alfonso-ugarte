/**
 * Dirección del backend (proyecto ASE252S3_BE). Con `docker compose up` o `mvn spring-boot:run`
 * la API queda en http://localhost:8080 y acepta peticiones desde http://localhost:4200.
 */
export const API_URL = 'http://localhost:8080/api';

/**
 * De dónde salen los datos:
 * - 'auto': usa el backend si responde; si no, el modo local (sin servidor).
 * - 'servidor': siempre el backend.
 * - 'local': siempre el modo local (datos de demostración guardados en este navegador).
 * En la pantalla de ingreso también se puede cambiar el modo.
 */
export const MODO_DATOS: 'auto' | 'servidor' | 'local' = 'auto';
