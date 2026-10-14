package pe.edu.vallegrande.app.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

import pe.edu.vallegrande.app.model.Usuario;
import pe.edu.vallegrande.app.model.enums.EstadoUsuario;
import pe.edu.vallegrande.app.model.enums.Perfil;

/** Equivale a UsuarioSistema del frontend (nunca incluye la contraseña). */
public record UsuarioResponse(
        Long id,
        String nombre,
        String correo,
        Perfil perfil,
        EstadoUsuario estado,
        LocalDate creado,
        LocalDateTime ultimoAcceso,
        Long docenteId,
        Long estudianteId) {

    public static UsuarioResponse de(Usuario u, Long docenteId, Long estudianteId) {
        return new UsuarioResponse(u.getId(), u.getNombre(), u.getCorreo(), u.getPerfil(), u.getEstado(),
                u.getCreado(), u.getUltimoAcceso(), docenteId, estudianteId);
    }
}
