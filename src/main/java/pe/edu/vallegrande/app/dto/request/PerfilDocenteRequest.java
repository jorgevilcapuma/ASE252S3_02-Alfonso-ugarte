package pe.edu.vallegrande.app.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Datos que el docente puede editar en "Mi perfil". */
public record PerfilDocenteRequest(
        @NotBlank(message = "Ingresa tus nombres.") @Size(max = 80, message = "Los nombres admiten 80 caracteres.") String nombres,
        @NotBlank(message = "Ingresa tus apellidos.") @Size(max = 80, message = "Los apellidos admiten 80 caracteres.") String apellidos,
        @Pattern(regexp = "^$|^[0-9 +()-]{6,20}$", message = "Ingresa un teléfono válido.") String telefono) {
}
