package pe.edu.vallegrande.app.service;

import pe.edu.vallegrande.app.dto.request.ContactoRequest;
import pe.edu.vallegrande.app.dto.request.EstudianteRequest;
import pe.edu.vallegrande.app.dto.response.EstudianteResponse;

/** Datos personales del estudiante (el aula se gestiona con la matrícula). */
public interface EstudianteService {

    EstudianteResponse obtener(Long id);

    EstudianteResponse actualizar(Long id, EstudianteRequest request);

    EstudianteResponse actualizarContacto(Long id, ContactoRequest request);

    void eliminar(Long id);
}
