package pe.edu.vallegrande.app.service;

import java.util.List;

import pe.edu.vallegrande.app.dto.request.AsignacionRequest;
import pe.edu.vallegrande.app.dto.response.AsignacionResponse;

/** Cursos dictados por sección: docente, aula y horario. */
public interface AsignacionService {

    List<AsignacionResponse> listar(Long docenteId, Long seccionId);

    AsignacionResponse obtener(Long id);

    AsignacionResponse crear(AsignacionRequest request);

    AsignacionResponse actualizar(Long id, AsignacionRequest request);

    void eliminar(Long id);
}
