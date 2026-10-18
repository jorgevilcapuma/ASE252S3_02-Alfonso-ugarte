package pe.edu.vallegrande.app.dto.response;

import pe.edu.vallegrande.app.model.GradoSeccion;
import pe.edu.vallegrande.app.model.enums.Nivel;
import pe.edu.vallegrande.app.model.enums.Turno;

/** Aula con la cantidad de estudiantes activos. */
public record GradoSeccionResponse(
        Long id,
        Nivel nivel,
        String grado,
        String gradoEtiqueta,
        String seccion,
        Turno turno,
        String nombre,
        Long totalEstudiantes,
        String estado) {

    public static GradoSeccionResponse de(GradoSeccion g, Long totalEstudiantes) {
        return new GradoSeccionResponse(g.getId(), g.getNivel(), g.getGrado(), g.getGradoEtiqueta(), g.getSeccion(), g.getTurno(),
                g.getNombre(), totalEstudiantes, g.getEstado());
    }
}
