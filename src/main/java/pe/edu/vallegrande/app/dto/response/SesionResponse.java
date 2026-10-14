package pe.edu.vallegrande.app.dto.response;

import pe.edu.vallegrande.app.model.enums.Perfil;
import pe.edu.vallegrande.app.seguridad.SesionUsuario;

/** Equivale a SesionDemo del frontend, más la ruta inicial del perfil. */
public record SesionResponse(
        Long usuarioId,
        String correo,
        String nombre,
        Perfil perfil,
        Long estudianteId,
        Long docenteId,
        String dni,
        String rutaInicial) {

    public static SesionResponse de(SesionUsuario sesion) {
        String ruta = switch (sesion.perfil()) {
            case ADMINISTRADOR -> "/administrador/dashboard";
            case DOCENTE -> "/docente/dashboard";
            case ALUMNO -> "/alumno/inicio";
        };
        return new SesionResponse(sesion.usuarioId(), sesion.correo(), sesion.nombre(), sesion.perfil(),
                sesion.estudianteId(), sesion.docenteId(), sesion.dni(), ruta);
    }
}
