package pe.edu.vallegrande.app.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record NivelRequest(
        @NotBlank(message = "Ingresa el nombre del nivel.") @Size(max = 30, message = "El nombre del nivel admite 30 caracteres.") String nombre,
        @NotNull(message = "Indica el orden del nivel.") @Min(value = 1, message = "El orden debe ser mayor que cero.") Integer orden) {
}
