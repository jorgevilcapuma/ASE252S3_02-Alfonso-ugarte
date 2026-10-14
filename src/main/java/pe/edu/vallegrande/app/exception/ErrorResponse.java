package pe.edu.vallegrande.app.exception;

import java.time.LocalDateTime;
import java.util.List;

/** Cuerpo de todas las respuestas de error: un mensaje principal y, si aplica, el detalle por campo. */
public record ErrorResponse(int status, String mensaje, List<String> errores, String ruta, LocalDateTime fecha) {

    public static ErrorResponse de(int status, String mensaje, List<String> errores, String ruta) {
        return new ErrorResponse(status, mensaje, errores == null || errores.isEmpty() ? List.of(mensaje) : errores, ruta, LocalDateTime.now());
    }
}
