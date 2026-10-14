package pe.edu.vallegrande.app.dto.response;

import java.time.LocalDate;

import pe.edu.vallegrande.app.model.Docente;

/** Equivale a PerfilDocente del frontend. */
public record DocenteResponse(
        Long id,
        String nombres,
        String apellidos,
        String nombreCompleto,
        String dni,
        String email,
        String telefono,
        String especialidad,
        String colegiatura,
        String codigoPlaza,
        String condicion,
        String nivelEducativo,
        LocalDate fechaIngreso,
        Long usuarioId) {

    public static DocenteResponse de(Docente d) {
        return new DocenteResponse(d.getId(), d.getNombres(), d.getApellidos(), d.getNombreCompleto(), d.getDni(),
                d.getUsuario() == null ? null : d.getUsuario().getCorreo(),
                d.getTelefono(), d.getEspecialidad(), d.getColegiatura(), d.getCodigoPlaza(), d.getCondicion(),
                d.getNivelEducativo(), d.getFechaIngreso(),
                d.getUsuario() == null ? null : d.getUsuario().getId());
    }
}
