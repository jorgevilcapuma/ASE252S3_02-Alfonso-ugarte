package pe.edu.vallegrande.app.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import pe.edu.vallegrande.app.model.enums.Nivel;
import pe.edu.vallegrande.app.model.enums.Turno;

/** Si no se indica el turno se aplica la regla del colegio: la sección C funciona en la tarde. */
public record GradoSeccionRequest(
        @NotNull(message = "Selecciona el nivel (Inicial, Primaria o Secundaria).") Nivel nivel,
        @NotBlank(message = "Ingresa el grado.") @Size(max = 10, message = "El grado admite 10 caracteres.") String grado,
        @NotBlank(message = "Ingresa la letra de la sección.")
        @Pattern(regexp = "[A-Za-z]", message = "La sección es una sola letra (A, B, C...).") String seccion,
        Turno turno) {
}
