/**
 * Reglas de validación de los formularios. Son las mismas del backend (util/Patrones.java y los DTO), para avisar
 * al instante con el mismo mensaje; el backend vuelve a validar todo al guardar.
 * Cada regla devuelve el mensaje de error o null si el valor es válido.
 */

const NOMBRE = /^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ][A-Za-zÁÉÍÓÚÜÑáéíóúüñ' .-]*$/;
const CORREO = /^[^\s@]+@[^\s@]+\.[A-Za-z]{2,}$/;
const vacio = (valor: string | null | undefined) => !valor || !valor.trim();

export const MENSAJE_NOMBRE = 'Los nombres y apellidos solo admiten letras, espacios, apóstrofo o guion.';
export const MENSAJE_CORREO = 'Ingresa un correo válido, por ejemplo nombre@ie20191.edu.pe.';

/** Nombres o apellidos: obligatorios, de 2 a 80 caracteres y solo letras. */
export function nombre(valor: string, etiqueta = 'Los nombres'): string | null {
  const texto = (valor ?? '').trim();
  if (!texto) return `${etiqueta} son obligatorios.`;
  if (!NOMBRE.test(texto)) return MENSAJE_NOMBRE;
  if (texto.length < 2 || texto.length > 80) return `${etiqueta} deben tener entre 2 y 80 caracteres.`;
  return null;
}

/** Nombre completo de una cuenta (al menos nombre y apellido). */
export function nombreCompleto(valor: string): string | null {
  const texto = (valor ?? '').trim().replace(/\s+/g, ' ');
  if (!texto) return 'Ingresa nombres y apellidos.';
  if (!NOMBRE.test(texto)) return MENSAJE_NOMBRE;
  if (texto.split(' ').length < 2) return 'Ingresa nombres y apellidos.';
  return null;
}

export function dni(valor: string): string | null {
  return /^\d{8}$/.test((valor ?? '').trim()) ? null : 'El DNI debe tener exactamente 8 dígitos.';
}

/** Correo opcional: si se escribe, debe tener dominio completo. */
export function correoOpcional(valor: string): string | null {
  return vacio(valor) || CORREO.test(valor.trim()) ? null : MENSAJE_CORREO;
}

export function correo(valor: string): string | null {
  return vacio(valor) ? 'Ingresa el correo.' : correoOpcional(valor);
}

/** Celular peruano opcional: 9 dígitos que empiezan con 9. */
export function celularOpcional(valor: string): string | null {
  return vacio(valor) || /^9\d{8}$/.test(valor.trim()) ? null : 'El celular debe tener 9 dígitos y empezar con 9.';
}

/** Teléfono opcional: dígitos, espacios, +, paréntesis o guion (6 a 20). */
export function telefonoOpcional(valor: string): string | null {
  return vacio(valor) || /^[0-9 +()-]{6,20}$/.test(valor.trim()) ? null : 'Ingresa un teléfono válido (6 a 20 dígitos).';
}

/** Texto obligatorio con un mínimo de caracteres útiles. */
export function texto(valor: string, minimo: number, etiqueta: string): string | null {
  const limpio = (valor ?? '').trim();
  if (!limpio) return `${etiqueta} es obligatorio.`;
  return limpio.length < minimo ? `${etiqueta} debe tener al menos ${minimo} caracteres.` : null;
}

export function fechaNoFutura(fecha: string, hoy: string): string | null {
  return fecha && fecha > hoy ? 'La fecha no puede ser futura.' : null;
}

/** Día de clases: lunes a viernes. */
export function diaHabil(fecha: string): string | null {
  if (!fecha) return 'Indica la fecha.';
  const dia = new Date(`${fecha}T12:00:00`).getDay();
  return dia === 0 || dia === 6 ? 'La fecha debe ser un día de clases (lunes a viernes).' : null;
}

/** Grados de cada nivel: Inicial 3 a 5 años, Primaria 1 a 6, Secundaria 1 a 5. */
export function gradoEnNivel(nivel: string, grado: string): string | null {
  const numero = Number((grado ?? '').trim().replace(/\s*(años?|°|º|ro|do|to|er)\s*$/i, ''));
  if (nivel === 'Inicial') return numero >= 3 && numero <= 5 && Number.isInteger(numero) ? null : 'En Inicial el grado es 3, 4 o 5 años.';
  if (nivel === 'Primaria') return numero >= 1 && numero <= 6 && Number.isInteger(numero) ? null : 'En Primaria el grado va de 1 a 6.';
  if (nivel === 'Secundaria') return numero >= 1 && numero <= 5 && Number.isInteger(numero) ? null : 'En Secundaria el grado va de 1 a 5.';
  return 'Selecciona el nivel.';
}

export function seccion(valor: string): string | null {
  return /^[A-Za-z]$/.test((valor ?? '').trim()) ? null : 'La sección es una sola letra (A, B, C…).';
}

/** Primer mensaje de error de una lista de reglas ya evaluadas. */
export function primero(...resultados: (string | null)[]): string | null {
  return resultados.find(Boolean) ?? null;
}
