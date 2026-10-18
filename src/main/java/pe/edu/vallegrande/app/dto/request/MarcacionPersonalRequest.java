package pe.edu.vallegrande.app.dto.request;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.constraints.Size;

/**
 * Marcación de entrada o salida. Sin fecha se usa hoy; sin hora, la hora actual
 * (para un día anterior la hora es obligatoria, porque es una corrección).
 */
public record MarcacionPersonalRequest(
        LocalDate fecha,
        LocalTime hora,
        @Size(max = 200, message = "La observación admite hasta 200 caracteres.") String observacion) {
}
