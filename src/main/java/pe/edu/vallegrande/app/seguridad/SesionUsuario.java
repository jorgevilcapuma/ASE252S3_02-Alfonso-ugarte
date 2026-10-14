package pe.edu.vallegrande.app.seguridad;

import pe.edu.vallegrande.app.exception.ApiException;
import pe.edu.vallegrande.app.model.enums.Perfil;

/**
 * Datos de la sesión firmados dentro del token. Para el perfil alumno {@code usuarioId}
 * puede ser null (ingresa con nombre y DNI) y {@code estudianteId} identifica al alumno.
 */
public record SesionUsuario(
        Long usuarioId,
        String correo,
        String nombre,
        Perfil perfil,
        Long estudianteId,
        Long docenteId,
        String dni,
        long expiraEn) {

    public boolean esAdministrador() {
        return perfil == Perfil.ADMINISTRADOR;
    }

    public boolean esDocente() {
        return perfil == Perfil.DOCENTE;
    }

    public boolean esAlumno() {
        return perfil == Perfil.ALUMNO;
    }

    /** Un alumno solo puede consultar o modificar sus propios datos. */
    public void verificarEstudiante(Long estudianteId) {
        if (esAlumno() && !estudianteId.equals(this.estudianteId)) {
            throw ApiException.prohibido("Solo puedes consultar tu propia información.");
        }
    }

    /** Filtro de "solo lo mío" para un docente: una cuenta sin ficha vinculada no ve ningún curso (nunca null = todos). */
    public Long docenteFiltro() {
        return docenteId == null ? -1L : docenteId;
    }

    /** Un docente solo puede gestionar su propia ficha y sus cursos. */
    public void verificarDocente(Long docenteId) {
        if (esDocente() && !docenteId.equals(this.docenteId)) {
            throw ApiException.prohibido("Solo puedes gestionar tu propia información y tus cursos.");
        }
    }
}
