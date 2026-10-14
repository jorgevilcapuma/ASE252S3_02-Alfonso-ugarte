package pe.edu.vallegrande.app.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Corrección de los datos personales del estudiante (el aula se cambia con un traslado). */
public record EstudianteRequest(
        @NotBlank(message = "Ingresa los nombres.") @Size(max = 80, message = "Los nombres admiten 80 caracteres.") String nombres,
        @NotBlank(message = "Ingresa los apellidos.") @Size(max = 80, message = "Los apellidos admiten 80 caracteres.") String apellidos,
        @NotNull(message = "El DNI debe tener 8 dígitos.") @Pattern(regexp = "\\d{8}", message = "El DNI debe tener 8 dígitos.") String dni,
        @Pattern(regexp = "^$|^[MF]$", message = "El género debe ser M o F.") String genero,
        @Pattern(regexp = "^$|^[0-9 +()-]{6,20}$", message = "Ingresa un teléfono válido.") String telefono,
        @Email(message = "Ingresa un correo válido.") @Size(max = 120, message = "El correo admite 120 caracteres.") String correo) {
}
