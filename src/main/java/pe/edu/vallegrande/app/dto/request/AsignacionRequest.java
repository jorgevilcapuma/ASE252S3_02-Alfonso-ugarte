package pe.edu.vallegrande.app.dto.request;

import java.time.LocalTime;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pe.edu.vallegrande.app.model.enums.DiaSemana;

public record AsignacionRequest(
        @NotBlank(message = "Ingresa el código del curso (por ejemplo MAT-3A).") @Size(max = 20, message = "El código admite 20 caracteres.") String codigo,
        @NotNull(message = "Selecciona el curso.") Long cursoId,
        @NotNull(message = "Selecciona la sección.") Long seccionId,
        @NotNull(message = "Selecciona el docente.") Long docenteId,
        @Size(max = 60, message = "El aula admite 60 caracteres.") String aula,
        @Valid List<HorarioRequest> horarios) {

    public record HorarioRequest(
            @NotNull(message = "Indica el día del horario.") DiaSemana dia,
            @NotNull(message = "Indica la hora de inicio (HH:mm).") LocalTime inicio,
            @NotNull(message = "Indica la hora de fin (HH:mm).") LocalTime fin) {
    }
}
