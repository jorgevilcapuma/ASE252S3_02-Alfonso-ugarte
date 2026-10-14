package pe.edu.vallegrande.app.dto.response;

import java.time.LocalDate;
import java.util.List;

import pe.edu.vallegrande.app.dto.response.AlertaResponses.FaltasDia;

/** Resumen institucional del día (dashboard del administrador). */
public record DashboardResponse(
        LocalDate fecha,
        long totalEstudiantes,
        int porcentajeRegistro,
        int presentesHoy,
        int tardanzasHoy,
        int faltasHoy,
        int justificadosHoy,
        long justificacionesPendientes,
        int alertasCriticas,
        int alertasPreventivas,
        List<NivelResumen> porNivel,
        List<FaltasDia> tendencia) {

    public record NivelResumen(String nivel, long estudiantes, long registrados, int porcentajeRegistro) {
    }
}
