package pe.edu.vallegrande.app.service;

import java.time.LocalDate;
import java.util.List;

import pe.edu.vallegrande.app.dto.request.AsistenciaLoteRequest;
import pe.edu.vallegrande.app.dto.response.AsistenciaResponses.AsistenciaDiaResponse;
import pe.edu.vallegrande.app.dto.response.AsistenciaResponses.MatrizMensualResponse;
import pe.edu.vallegrande.app.dto.response.AsistenciaResponses.RegistroDia;
import pe.edu.vallegrande.app.dto.response.AsistenciaResponses.RegistroHistorial;
import pe.edu.vallegrande.app.model.enums.EstadoAsistencia;
import pe.edu.vallegrande.app.seguridad.SesionUsuario;

/** Asistencia diaria por aula (matriz P/T/F/J). */
public interface AsistenciaService {

    AsistenciaDiaResponse delAula(Long seccionId, LocalDate fecha);

    MatrizMensualResponse mensual(Long seccionId, String mes);

    AsistenciaDiaResponse registrarLote(Long seccionId, AsistenciaLoteRequest request, SesionUsuario sesion);

    /** Guarda el estado de un estudiante en una fecha; "pendiente" elimina el registro. */
    RegistroHistorial registrar(Long estudianteId, LocalDate fecha, EstadoAsistencia estado, SesionUsuario sesion);

    List<RegistroHistorial> historial(Long estudianteId, LocalDate desde, LocalDate hasta);

    /** Registros de todas las matrículas activas (o de una sección) entre dos fechas; máximo 93 días. */
    List<RegistroDia> rango(LocalDate desde, LocalDate hasta, Long seccionId);
}
