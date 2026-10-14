package pe.edu.vallegrande.app.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pe.edu.vallegrande.app.model.enums.Prioridad;

/** Sin {@code cursoId} el comunicado es para todos los cursos. */
public record ComunicadoRequest(
        @NotBlank(message = "Ingresa el título del comunicado.") @Size(max = 150, message = "El título admite 150 caracteres.") String titulo,
        @NotBlank(message = "Ingresa el contenido del comunicado.") @Size(max = 2000, message = "El contenido admite 2000 caracteres.") String contenido,
        @NotNull(message = "Selecciona la prioridad (Alta, Media o Baja).") Prioridad prioridad,
        Long cursoId) {
}
