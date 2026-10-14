/** Error con el mensaje legible que devuelve la API ({ mensaje, errores[] }), del servidor o del modo local. */
export class ErrorApi extends Error {
  constructor(readonly status: number, mensaje: string, readonly errores: string[] = []) {
    super(mensaje);
  }
}
