package pe.edu.vallegrande.app.dto.request;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import pe.edu.vallegrande.app.model.enums.EstadoAsistencia;

/** Registro de la matriz diaria: varios estudiantes en una fecha. "pendiente" borra el registro del día. */
public record AsistenciaLoteRequest(
        @NotNull(message = "Indica la fecha de la asistencia.") LocalDate fecha,
        @NotEmpty(message = "Envía al menos un registro de asistencia.") @Valid List<Registro> registros) {

    public record Registro(
            @NotNull(message = "Cada registro necesita el estudiante.") Long estudianteId,
            @NotNull(message = "Cada registro necesita un estado (presente, tardanza, falta, justificado).") EstadoAsistencia estado) {
    }
}
