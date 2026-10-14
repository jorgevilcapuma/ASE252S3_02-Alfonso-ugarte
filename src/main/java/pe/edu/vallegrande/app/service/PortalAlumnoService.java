package pe.edu.vallegrande.app.service;

import java.time.LocalDate;
import java.util.List;

import pe.edu.vallegrande.app.dto.response.AsignacionResponse;
import pe.edu.vallegrande.app.dto.response.EstudianteResponse;
import pe.edu.vallegrande.app.dto.response.PortalAlumnoResponses.AvisoAlumnoResponse;
import pe.edu.vallegrande.app.dto.response.PortalAlumnoResponses.DiaAlumnoResponse;
import pe.edu.vallegrande.app.dto.response.PortalAlumnoResponses.InicioAlumnoResponse;
import pe.edu.vallegrande.app.dto.response.PortalAlumnoResponses.ResumenAsistenciaResponse;

/** Pantallas del perfil alumno: inicio, mis cursos, horario, mi asistencia, alertas y comunicaciones. */
public interface PortalAlumnoService {

    EstudianteResponse perfil(Long estudianteId);

    InicioAlumnoResponse inicio(Long estudianteId);

    List<AsignacionResponse> cursos(Long estudianteId);

    List<DiaAlumnoResponse> asistencia(Long estudianteId, Long cursoId, LocalDate desde, LocalDate hasta, Integer limite);

    ResumenAsistenciaResponse resumen(Long estudianteId, Long cursoId, LocalDate desde, LocalDate hasta);

    List<AvisoAlumnoResponse> alertas(Long estudianteId);

    List<AvisoAlumnoResponse> comunicaciones(Long estudianteId);
}
