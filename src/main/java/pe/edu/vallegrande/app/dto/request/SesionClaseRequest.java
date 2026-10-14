package pe.edu.vallegrande.app.dto.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pe.edu.vallegrande.app.model.enums.EstadoAsistenciaCurso;

/** Asistencia de un curso en una fecha ("Asistencia por curso" del docente). */
public record SesionClaseRequest(
        @NotEmpty(message = "Envía la asistencia de al menos un alumno.") @Valid List<Registro> registros) {

    public record Registro(
            @NotNull(message = "Cada registro necesita el alumno.") Long estudianteId,
            @NotNull(message = "Cada registro necesita un estado (asistio, tardanza, falta).") EstadoAsistenciaCurso estado,
            @Size(max = 255, message = "La observación admite 255 caracteres.") String observacion) {
    }
}
