package pe.edu.vallegrande.app.service;

import java.util.List;

import pe.edu.vallegrande.app.dto.request.ContactoRequest;
import pe.edu.vallegrande.app.dto.request.EstudianteRequest;
import pe.edu.vallegrande.app.dto.response.EstudianteResponse;

public interface EstudianteService {

    /** estado: A (activos, por defecto), I (retirados) o T (todos); texto busca en nombres, apellidos o DNI. */
    List<EstudianteResponse> listar(Long gradoSeccionId, String estado, String texto);

    EstudianteResponse obtener(Long id);

    EstudianteResponse crear(EstudianteRequest request);

    EstudianteResponse actualizar(Long id, EstudianteRequest request);

    EstudianteResponse actualizarContacto(Long id, ContactoRequest request);

    /** Retiro lógico (estado I): conserva su historial de asistencia. */
    EstudianteResponse eliminar(Long id);

    EstudianteResponse restaurar(Long id);
}
