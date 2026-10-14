package pe.edu.vallegrande.app.service;

import java.time.LocalDate;
import java.util.List;

import pe.edu.vallegrande.app.dto.request.SesionClaseRequest;
import pe.edu.vallegrande.app.dto.response.SesionClaseResponses.ResumenMensualResponse;
import pe.edu.vallegrande.app.dto.response.SesionClaseResponses.SesionClaseResponse;
import pe.edu.vallegrande.app.dto.response.SesionClaseResponses.SesionHistorialResponse;
import pe.edu.vallegrande.app.seguridad.SesionUsuario;

/** Asistencia por curso: una sesión por curso y fecha. */
public interface SesionClaseService {

    SesionClaseResponse obtener(Long cursoId, LocalDate fecha, SesionUsuario sesion);

    SesionClaseResponse guardar(Long cursoId, LocalDate fecha, SesionClaseRequest request, SesionUsuario sesion);

    void eliminar(Long cursoId, LocalDate fecha, SesionUsuario sesion);

    List<SesionHistorialResponse> historial(Long docenteId, Long cursoId, LocalDate desde, LocalDate hasta);

    List<ResumenMensualResponse> resumenMensual(Long cursoId, String mes, SesionUsuario sesion);
}
