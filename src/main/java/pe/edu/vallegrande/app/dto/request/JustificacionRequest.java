package pe.edu.vallegrande.app.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Solicitud de justificación. Si la envía un alumno, {@code estudianteId} se toma de su sesión. */
public record JustificacionRequest(
        Long estudianteId,
        @NotNull(message = "Indica la fecha de la inasistencia.") LocalDate fecha,
        Long cursoId,
        @NotBlank(message = "Describe el motivo de la inasistencia.") @Size(min = 5, max = 200, message = "Describe el motivo con al menos 5 caracteres (máximo 200).") String motivo,
        @Size(max = 500, message = "El detalle admite 500 caracteres.") String detalle,
        @Size(max = 255, message = "El nombre del archivo admite 255 caracteres.") String archivoNombre) {
}
