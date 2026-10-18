package pe.edu.vallegrande.app.dto.response;

import pe.edu.vallegrande.app.model.Curso;

/** Curso con su aula y su docente. */
public record CursoResponse(
        Long id,
        String nombre,
        String nombreConAula,
        Long gradoSeccionId,
        String aula,
        Long docenteId,
        String docenteNombre,
        String estado) {

    public static CursoResponse de(Curso c) {
        return new CursoResponse(c.getId(), c.getNombre(), c.getNombreConAula(), c.getGradoSeccion().getId(),
                c.getGradoSeccion().getNombre(), c.getDocente().getId(), c.getDocente().getNombreCompleto(), c.getEstado());
    }
}
