package pe.edu.vallegrande.app.dto.response;

import pe.edu.vallegrande.app.model.Curso;

public record CursoResponse(Long id, String nombre, String descripcion) {

    public static CursoResponse de(Curso c) {
        return new CursoResponse(c.getId(), c.getNombre(), c.getDescripcion());
    }
}
