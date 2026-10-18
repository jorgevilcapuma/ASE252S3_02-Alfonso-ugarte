package pe.edu.vallegrande.app.service;

import java.util.List;

import pe.edu.vallegrande.app.dto.request.CursoRequest;
import pe.edu.vallegrande.app.dto.response.CursoResponse;

public interface CursoService {

    List<CursoResponse> listar(Long gradoSeccionId, Long docenteId, String estado);

    CursoResponse obtener(Long id);

    CursoResponse crear(CursoRequest request);

    CursoResponse actualizar(Long id, CursoRequest request);

    CursoResponse eliminar(Long id);

    CursoResponse restaurar(Long id);
}
