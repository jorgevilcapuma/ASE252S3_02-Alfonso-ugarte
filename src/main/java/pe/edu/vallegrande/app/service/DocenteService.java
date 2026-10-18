package pe.edu.vallegrande.app.service;

import java.util.List;

import pe.edu.vallegrande.app.dto.request.DocenteRequest;
import pe.edu.vallegrande.app.dto.response.DocenteResponse;

/** Docentes y demás personal del colegio (tabla docentes). */
public interface DocenteService {

    /** estado: A (activos), I (inactivos) o null/T (todos); cargo opcional. */
    List<DocenteResponse> listar(String estado, String cargo);

    DocenteResponse obtener(Long id);

    DocenteResponse crear(DocenteRequest request);

    DocenteResponse editar(Long id, DocenteRequest request);

    /** Eliminación lógica (estado I). */
    DocenteResponse eliminar(Long id);

    /** Restauración lógica (estado A). */
    DocenteResponse restaurar(Long id);
}
