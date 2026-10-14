package pe.edu.vallegrande.app.service;

import java.util.List;

import pe.edu.vallegrande.app.dto.request.PersonalRequest;
import pe.edu.vallegrande.app.dto.response.PersonalResponse;

/** Tabla maestra del personal con eliminación y restauración lógicas. */
public interface PersonalService {

    List<PersonalResponse> listar();

    /** A = activos, I = inactivos, T = todos. */
    List<PersonalResponse> listarPorEstado(String estado);

    PersonalResponse obtener(Long id);

    PersonalResponse crear(PersonalRequest request);

    PersonalResponse editar(Long id, PersonalRequest request);

    PersonalResponse eliminar(Long id);

    PersonalResponse restaurar(Long id);
}
