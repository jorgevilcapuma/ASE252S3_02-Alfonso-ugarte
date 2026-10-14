package pe.edu.vallegrande.app.dto.response;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import pe.edu.vallegrande.app.model.enums.EstadoAsistencia;
import pe.edu.vallegrande.app.model.enums.Turno;

/** Respuestas de la asistencia diaria por aula. */
public final class AsistenciaResponses {

    private AsistenciaResponses() {
    }

    public record AulaResponse(Long seccionId, String nivel, String grado, String gradoEtiqueta, String seccion, Turno turno) {
    }

    public record ConteoEstados(int presente, int tardanza, int falta, int justificado, int pendiente) {

        public static ConteoEstados de(java.util.Collection<EstadoAsistencia> estados) {
            int[] c = new int[5];
            for (EstadoAsistencia e : estados) {
                c[e.ordinal()]++;
            }
            return new ConteoEstados(c[0], c[1], c[2], c[3], c[4]);
        }
    }

    /** Registro del día en un aula (pantalla matriz-asistencia, vista diaria). */
    public record AsistenciaDiaResponse(AulaResponse aula, LocalDate fecha, List<RegistroEstudiante> registros, ConteoEstados resumen) {
    }

    public record RegistroEstudiante(Long estudianteId, String nombre, String dni, EstadoAsistencia estado) {
    }

    /** Vista mensual: un mapa fecha → estado por estudiante; los fines de semana se informan aparte. */
    public record MatrizMensualResponse(AulaResponse aula, String mes, List<LocalDate> dias, List<LocalDate> finesDeSemana,
                                        List<FilaMensual> filas) {
    }

    public record FilaMensual(Long estudianteId, String nombre, String dni, Map<LocalDate, EstadoAsistencia> estados,
                              ConteoEstados resumen) {
    }

    public record RegistroHistorial(LocalDate fecha, EstadoAsistencia estado) {
    }

    /** Fila compacta para consultas por rango (dashboard, alertas, matriz mensual). */
    public record RegistroDia(Long estudianteId, LocalDate fecha, EstadoAsistencia estado) {
    }
}
