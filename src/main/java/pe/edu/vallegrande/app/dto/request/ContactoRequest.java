package pe.edu.vallegrande.app.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Contacto editable por el alumno en "Mi perfil". */
public record ContactoRequest(
        @Pattern(regexp = "^$|^[0-9 +()-]{6,20}$", message = "Ingresa un teléfono válido.") String telefono,
        @Email(message = "Ingresa un correo válido.") @Size(max = 120, message = "El correo admite 120 caracteres.") String correo) {
}
