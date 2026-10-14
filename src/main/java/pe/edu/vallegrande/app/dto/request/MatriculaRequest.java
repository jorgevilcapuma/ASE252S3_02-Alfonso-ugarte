package pe.edu.vallegrande.app.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Matrícula manual. El nombre puede llegar completo en {@code nombre} (como en el formulario del frontend)
 * o separado en {@code nombres} y {@code apellidos}. El aula se indica con {@code seccionId}
 * o con nivel + grado + sección ("Primaria", "3", "A").
 */
public record MatriculaRequest(
        @Size(max = 160, message = "El nombre admite 160 caracteres.") String nombre,
        @Size(max = 80, message = "Los nombres admiten 80 caracteres.") String nombres,
        @Size(max = 80, message = "Los apellidos admiten 80 caracteres.") String apellidos,
        @NotNull(message = "El DNI debe tener 8 dígitos.") @Pattern(regexp = "\\d{8}", message = "El DNI debe tener 8 dígitos.") String dni,
        @Pattern(regexp = "^$|^[MF]$", message = "El género debe ser M o F.") String genero,
        String nivel,
        String grado,
        String seccion,
        Long seccionId,
        @Pattern(regexp = "^$|^[0-9 +()-]{6,20}$", message = "Ingresa un teléfono válido.") String telefono,
        @Email(message = "Ingresa un correo válido.") @Size(max = 120, message = "El correo admite 120 caracteres.") String correo) {
}
