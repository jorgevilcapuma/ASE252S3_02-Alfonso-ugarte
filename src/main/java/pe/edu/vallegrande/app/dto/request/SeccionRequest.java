package pe.edu.vallegrande.app.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import pe.edu.vallegrande.app.model.enums.Turno;

/** Si no se indica el turno se aplica la regla del colegio: la sección C funciona en la tarde. */
public record SeccionRequest(
        @NotNull(message = "Selecciona el grado de la sección.") Long gradoId,
        @NotBlank(message = "Ingresa la letra de la sección.")
        @Pattern(regexp = "[A-Za-z]", message = "La sección es una sola letra (A, B, C...).") String letra,
        Turno turno) {
}
