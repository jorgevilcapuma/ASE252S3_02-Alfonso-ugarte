package pe.edu.vallegrande.app.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pe.edu.vallegrande.app.model.enums.EstadoUsuario;
import pe.edu.vallegrande.app.model.enums.Perfil;
import pe.edu.vallegrande.app.util.Patrones;
import jakarta.validation.constraints.Pattern;

/**
 * Alta o edición de una cuenta. Sin contraseña al crear se usa la contraseña inicial configurada;
 * al editar, una contraseña vacía conserva la actual. docenteId/estudianteId vinculan la cuenta a una ficha.
 */
public record UsuarioRequest(
        @NotBlank(message = "Ingresa nombres y apellidos.")
        @Pattern(regexp = Patrones.NOMBRE, message = Patrones.NOMBRE_MENSAJE) @Size(max = 120, message = "El nombre admite 120 caracteres.") String nombre,
        @NotBlank(message = "Ingresa un correo válido.")
        @Pattern(regexp = Patrones.CORREO, message = Patrones.CORREO_MENSAJE) @Size(max = 120, message = "El correo admite 120 caracteres.") String correo,
        @NotNull(message = "Selecciona el perfil de la cuenta.") Perfil perfil,
        EstadoUsuario estado,
        @Size(min = 6, max = 100, message = "La contraseña debe tener 6 caracteres como mínimo.") String contrasena,
        Long docenteId,
        Long estudianteId) {
}
