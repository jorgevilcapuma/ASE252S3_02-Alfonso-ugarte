package pe.edu.vallegrande.app.service;

import java.time.LocalDate;
import java.util.List;

import pe.edu.vallegrande.app.dto.request.JustificacionRequest;
import pe.edu.vallegrande.app.dto.request.RevisionJustificacionRequest;
import pe.edu.vallegrande.app.dto.response.JustificacionResponse;
import pe.edu.vallegrande.app.model.enums.EstadoJustificacion;
import pe.edu.vallegrande.app.seguridad.SesionUsuario;

public interface JustificacionService {

    List<JustificacionResponse> listar(EstadoJustificacion estado, Long estudianteId, Long cursoId, Long docenteId,
                                       LocalDate desde, LocalDate hasta, SesionUsuario sesion);

    JustificacionResponse obtener(Long id, SesionUsuario sesion);

    JustificacionResponse crear(JustificacionRequest request, SesionUsuario sesion);

    /** Solo mientras está pendiente. */
    JustificacionResponse actualizar(Long id, JustificacionRequest request, SesionUsuario sesion);

    /**
     * Aprobar marca el día como justificado (salvo que el estudiante estuviera presente).
     * Observar devuelve el día a su estado previo; si no había uno válido, queda como falta.
     */
    JustificacionResponse revisar(Long id, RevisionJustificacionRequest request, SesionUsuario sesion);

    /** Solo mientras está pendiente (el alumno retira su solicitud). */
    void eliminar(Long id, SesionUsuario sesion);
}
