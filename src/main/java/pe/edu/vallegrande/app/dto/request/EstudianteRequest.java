package pe.edu.vallegrande.app.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import pe.edu.vallegrande.app.util.Patrones;

/**
 * Alta o edición de un estudiante con su aula (grado y sección).
 * usuarioId (opcional) vincula su cuenta de perfil alumno; sin él se conserva la cuenta ya vinculada.
 */
public record EstudianteRequest(
        @NotBlank(message = "Ingresa los nombres.")
        @Pattern(regexp = Patrones.NOMBRE, message = Patrones.NOMBRE_MENSAJE) @Size(min = 2, max = 80, message = "Los nombres deben tener entre 2 y 80 caracteres.") String nombres,
        @NotBlank(message = "Ingresa los apellidos.")
        @Pattern(regexp = Patrones.NOMBRE, message = Patrones.NOMBRE_MENSAJE) @Size(min = 2, max = 80, message = "Los apellidos deben tener entre 2 y 80 caracteres.") String apellidos,
        @NotNull(message = "El DNI debe tener 8 dígitos.") @Pattern(regexp = "\\d{8}", message = "El DNI debe tener 8 dígitos.") String dni,
        @NotNull(message = "Selecciona el grado y sección del estudiante.") Long gradoSeccionId,
        @Pattern(regexp = "^$|^[MF]$", message = "El género debe ser M o F.") String genero,
        @Pattern(regexp = "^$|^[0-9 +()-]{6,20}$", message = "Ingresa un teléfono válido.") String telefono,
        @Pattern(regexp = Patrones.CORREO, message = Patrones.CORREO_MENSAJE) @Size(max = 120, message = "El correo admite 120 caracteres.") String correo,
        Long usuarioId) {
}
