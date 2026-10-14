package pe.edu.vallegrande.app.exception;

import java.util.List;

import org.springframework.http.HttpStatus;

import lombok.Getter;

/** Error controlado de la API: se responde con su código HTTP y un mensaje legible. */
@Getter
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final List<String> errores;

    private ApiException(HttpStatus status, String mensaje, List<String> errores) {
        super(mensaje);
        this.status = status;
        this.errores = errores;
    }

    public static ApiException validacion(String mensaje) {
        return new ApiException(HttpStatus.BAD_REQUEST, mensaje, List.of(mensaje));
    }

    public static ApiException validacion(List<String> errores) {
        return new ApiException(HttpStatus.BAD_REQUEST, "Revisa los datos enviados.", errores);
    }

    public static ApiException noEncontrado(String mensaje) {
        return new ApiException(HttpStatus.NOT_FOUND, mensaje, List.of(mensaje));
    }

    public static ApiException conflicto(String mensaje) {
        return new ApiException(HttpStatus.CONFLICT, mensaje, List.of(mensaje));
    }

    public static ApiException noAutorizado(String mensaje) {
        return new ApiException(HttpStatus.UNAUTHORIZED, mensaje, List.of(mensaje));
    }

    public static ApiException prohibido(String mensaje) {
        return new ApiException(HttpStatus.FORBIDDEN, mensaje, List.of(mensaje));
    }
}
