package pe.edu.vallegrande.app.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pe.edu.vallegrande.app.model.enums.EstadoAsistencia;

/** Cambio de la asistencia de un estudiante en una fecha (sin cursoId: asistencia diaria). */
public record EstadoAsistenciaRequest(
        @NotNull(message = "Indica la fecha de la asistencia.") LocalDate fecha,
        Long cursoId,
        @NotNull(message = "Indica el estado (presente, tardanza, falta, justificado o pendiente).") EstadoAsistencia estado,
        @Size(max = 255, message = "La observación admite 255 caracteres.") String observacion) {
}
