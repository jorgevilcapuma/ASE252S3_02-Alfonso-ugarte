package pe.edu.vallegrande.app.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** La contraseña actual es obligatoria cuando el usuario cambia la suya (no cuando lo hace el administrador). */
public record CambioContrasenaRequest(
        String contrasenaActual,
        @NotBlank(message = "Ingresa la nueva contraseña.")
        @Size(min = 6, max = 100, message = "La contraseña debe tener 6 caracteres como mínimo.") String contrasenaNueva) {
}
