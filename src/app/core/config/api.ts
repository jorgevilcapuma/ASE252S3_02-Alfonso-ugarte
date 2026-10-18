/**
 * Dirección del backend (proyecto ASE252S3_BE). Con `docker compose up` o `mvn spring-boot:run`
 * la API queda en http://localhost:8080 y acepta peticiones desde http://localhost:4200.
 */
export const API_URL = 'http://localhost:8080/api';

/**
 * De dónde salen los datos (no se elige en la pantalla de ingreso):
 * - 'servidor': el backend (lo normal).
 * - 'local': datos de demostración guardados en el navegador, sin backend (solo para mostrar el portal sin servidor).
 * - 'auto': el backend si responde y, si no, el modo local.
 */
export const MODO_DATOS: 'auto' | 'servidor' | 'local' = 'servidor';

/**
 * Modo local: false empieza sin registros (solo la cuenta admin@ie20191.edu.pe / 123456 y las aulas);
 * true carga los datos de demostración (docentes, 505 estudiantes, cursos, historial).
 * Al cambiarlo, los datos guardados en el navegador se reemplazan en la siguiente carga.
 */
export const DATOS_DEMO_LOCAL = false;
