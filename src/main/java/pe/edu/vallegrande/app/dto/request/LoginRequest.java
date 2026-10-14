package pe.edu.vallegrande.app.dto.request;

import jakarta.validation.constraints.NotNull;
import pe.edu.vallegrande.app.model.enums.Perfil;

/**
 * Administrador y docente ingresan con correo y contraseña.
 * El alumno ingresa con nombre completo y DNI (o con correo y contraseña si tiene cuenta).
 */
public record LoginRequest(
        @NotNull(message = "Selecciona tu perfil de acceso.") Perfil perfil,
        String correo,
        String contrasena,
        String nombre,
        String dni) {
}
