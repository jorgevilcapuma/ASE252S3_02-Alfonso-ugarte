package pe.edu.vallegrande.app.dto.request;

import jakarta.validation.constraints.NotNull;
import pe.edu.vallegrande.app.model.enums.EstadoAsistencia;

public record EstadoAsistenciaRequest(
        @NotNull(message = "Indica el estado (presente, tardanza, falta, justificado o pendiente).") EstadoAsistencia estado) {
}
