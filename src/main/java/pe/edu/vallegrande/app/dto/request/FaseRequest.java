package pe.edu.vallegrande.app.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pe.edu.vallegrande.app.model.enums.EstadoFase;

public record FaseRequest(
        @NotNull(message = "Indica el orden de la fase.") @Min(value = 1, message = "El orden debe ser mayor que cero.") Integer orden,
        @NotBlank(message = "Indica la semana.") @Size(max = 10, message = "La semana admite 10 caracteres.") String semana,
        @NotBlank(message = "Ingresa el título de la fase.") @Size(max = 80, message = "El título admite 80 caracteres.") String titulo,
        @NotBlank(message = "Describe la actividad.") @Size(max = 300, message = "La actividad admite 300 caracteres.") String actividad,
        @NotBlank(message = "Indica el producto.") @Size(max = 150, message = "El producto admite 150 caracteres.") String producto,
        @NotNull(message = "Indica el estado (Completada, En curso, Pendiente).") EstadoFase estado) {
}
