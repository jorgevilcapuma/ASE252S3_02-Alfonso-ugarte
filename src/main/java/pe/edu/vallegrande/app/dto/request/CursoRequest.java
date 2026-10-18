package pe.edu.vallegrande.app.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Curso de un aula con el docente que lo dicta. */
public record CursoRequest(
        @NotBlank(message = "Ingresa el nombre del curso.") @Size(max = 120, message = "El nombre del curso admite 120 caracteres.") String nombre,
        @NotNull(message = "Selecciona el grado y sección del curso.") Long gradoSeccionId,
        @NotNull(message = "Selecciona el docente del curso.") Long docenteId) {
}
