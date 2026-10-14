package pe.edu.vallegrande.app.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pe.edu.vallegrande.app.model.enums.EstadoJustificacion;

public record RevisionJustificacionRequest(
        @NotNull(message = "Indica si la justificación se aprueba u observa.") EstadoJustificacion estado,
        @Size(max = 500, message = "La observación admite 500 caracteres.") String observacion) {
}
