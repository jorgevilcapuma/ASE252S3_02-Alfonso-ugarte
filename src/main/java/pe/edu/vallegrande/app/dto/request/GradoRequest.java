package pe.edu.vallegrande.app.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record GradoRequest(
        @NotNull(message = "Selecciona el nivel del grado.") Long nivelId,
        @NotBlank(message = "Ingresa el código del grado (por ejemplo 1 o 3 años).") @Size(max = 20, message = "El código del grado admite 20 caracteres.") String codigo,
        @NotBlank(message = "Ingresa la etiqueta del grado.") @Size(max = 40, message = "La etiqueta admite 40 caracteres.") String etiqueta,
        @NotNull(message = "Indica el orden del grado.") @Min(value = 1, message = "El orden debe ser mayor que cero.") Integer orden) {
}
