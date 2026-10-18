package pe.edu.vallegrande.app.dto.request;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pe.edu.vallegrande.app.model.enums.EstadoAsistencia;

/**
 * Asistencia de varios estudiantes de un aula en una fecha. Sin cursoId es la asistencia diaria;
 * con cursoId, la de esa clase. "pendiente" borra el registro.
 */
public record AsistenciaLoteRequest(
        @NotNull(message = "Indica la fecha de la asistencia.") LocalDate fecha,
        Long cursoId,
        @NotEmpty(message = "Envía al menos un registro de asistencia.") @Valid List<Registro> registros) {

    public record Registro(
            @NotNull(message = "Cada registro necesita el estudiante.") Long estudianteId,
            @NotNull(message = "Cada registro necesita un estado (presente, tardanza, falta, justificado).") EstadoAsistencia estado,
            @Size(max = 255, message = "La observación admite 255 caracteres.") String observacion) {
    }
}
