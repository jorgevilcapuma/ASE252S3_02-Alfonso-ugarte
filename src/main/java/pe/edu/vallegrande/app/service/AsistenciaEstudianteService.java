package pe.edu.vallegrande.app.service;

import java.time.LocalDate;
import java.util.List;

import pe.edu.vallegrande.app.dto.request.AsistenciaLoteRequest;
import pe.edu.vallegrande.app.dto.request.EstadoAsistenciaRequest;
import pe.edu.vallegrande.app.dto.response.AsistenciaResponses.AsistenciaDiaResponse;
import pe.edu.vallegrande.app.dto.response.AsistenciaResponses.MatrizMensualResponse;
import pe.edu.vallegrande.app.dto.response.AsistenciaResponses.RegistroHistorial;
import pe.edu.vallegrande.app.seguridad.SesionUsuario;

/** Asistencia de estudiantes: cursoId null = asistencia diaria del aula; con cursoId = asistencia de esa clase. */
public interface AsistenciaEstudianteService {

    AsistenciaDiaResponse delAula(Long gradoSeccionId, Long cursoId, LocalDate fecha);

    MatrizMensualResponse mensual(Long gradoSeccionId, Long cursoId, String mes);

    AsistenciaDiaResponse registrarLote(Long gradoSeccionId, AsistenciaLoteRequest request, SesionUsuario sesion);

    RegistroHistorial registrar(Long estudianteId, EstadoAsistenciaRequest request, SesionUsuario sesion);

    List<RegistroHistorial> historial(Long estudianteId, LocalDate desde, LocalDate hasta);
}
