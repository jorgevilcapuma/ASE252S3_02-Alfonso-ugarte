package pe.edu.vallegrande.app.service;

import java.util.List;

import pe.edu.vallegrande.app.dto.request.CursoRequest;
import pe.edu.vallegrande.app.dto.response.CursoResponse;

/** Tabla maestra de cursos / áreas curriculares. */
public interface CursoService {

    List<CursoResponse> listar();

    CursoResponse obtener(Long id);

    CursoResponse crear(CursoRequest request);

    CursoResponse actualizar(Long id, CursoRequest request);

    void eliminar(Long id);
}
