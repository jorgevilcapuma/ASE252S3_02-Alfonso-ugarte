package pe.edu.vallegrande.app.dto.response;

import java.time.LocalDate;

import pe.edu.vallegrande.app.model.Asignacion;
import pe.edu.vallegrande.app.model.Comunicado;
import pe.edu.vallegrande.app.model.enums.Prioridad;

/** Interfaz ComunicadoDocente del frontend. Sin curso, cursoNombre = "Todos los cursos". */
public record ComunicadoResponse(
        Long id,
        String titulo,
        String contenido,
        Long cursoId,
        String cursoNombre,
        Prioridad prioridad,
        LocalDate fechaCreacion,
        Long autorId,
        String autor) {

    public static ComunicadoResponse de(Comunicado c) {
        Asignacion a = c.getAsignacion();
        String curso = a == null ? "Todos los cursos"
                : a.getCurso().getNombre() + " - " + a.getSeccion().getGrado().getEtiqueta() + " " + a.getSeccion().getLetra();
        return new ComunicadoResponse(c.getId(), c.getTitulo(), c.getContenido(), a == null ? null : a.getId(), curso,
                c.getPrioridad(), c.getFechaCreacion(),
                c.getAutor() == null ? null : c.getAutor().getId(),
                c.getAutor() == null ? null : c.getAutor().getNombre());
    }
}
