package pe.edu.vallegrande.app.dto.response;

import pe.edu.vallegrande.app.model.Estudiante;
import pe.edu.vallegrande.app.model.GradoSeccion;
import pe.edu.vallegrande.app.model.enums.Nivel;
import pe.edu.vallegrande.app.model.enums.Turno;

/** Estudiante con su aula. */
public record EstudianteResponse(
        Long id,
        String nombre,
        String nombres,
        String apellidos,
        String dni,
        String genero,
        Long gradoSeccionId,
        Nivel nivel,
        String grado,
        String gradoEtiqueta,
        String seccion,
        Turno turno,
        String estado,
        String telefono,
        String correo,
        Long usuarioId) {

    public static EstudianteResponse de(Estudiante e) {
        GradoSeccion g = e.getGradoSeccion();
        return new EstudianteResponse(e.getId(), e.getNombreCompleto(), e.getNombres(), e.getApellidos(), e.getDni(), e.getGenero(),
                g.getId(), g.getNivel(), g.getGrado(), g.getGradoEtiqueta(), g.getSeccion(), g.getTurno(), e.getEstado(),
                e.getTelefono(), e.getCorreo(), e.getUsuario() == null ? null : e.getUsuario().getId());
    }
}
