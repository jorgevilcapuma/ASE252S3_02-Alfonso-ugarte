package pe.edu.vallegrande.app.dto.response;

import java.time.LocalDate;
import java.util.List;

import pe.edu.vallegrande.app.model.enums.EstadoAsistenciaCurso;

/** Respuestas de la asistencia por curso del docente. */
public final class SesionClaseResponses {

    private SesionClaseResponses() {
    }

    /** Lista para tomar asistencia: si la sesión no existe, cada alumno aparece como "asistio" y yaRegistrado = false. */
    public record SesionClaseResponse(Long asignacionId, String cursoNombre, String gradoSeccion, LocalDate fecha,
                                      boolean yaRegistrado, List<FilaAsistencia> filas, Conteo resumen) {
    }

    public record FilaAsistencia(Long alumnoId, String nombres, String apellidos, String dni, EstadoAsistenciaCurso estado,
                                 String observacion) {
    }

    public record Conteo(int asistieron, int tardanzas, int faltas, int justificados, int total) {
    }

    /** Interfaz SesionHistorial del frontend. */
    public record SesionHistorialResponse(Long id, LocalDate fecha, Long cursoId, String cursoNombre, String gradoSeccion,
                                          int asistieron, int tardanzas, int faltas, int justificados, int totalAlumnos) {
    }

    /** Reporte mensual por alumno (pantalla Reportes del docente). */
    public record ResumenMensualResponse(Long alumnoId, String nombres, String apellidos, String dni, int sesiones,
                                         int asistencias, int tardanzas, int faltas, int porcentaje) {
    }

    /** Interfaz AlertaAsistencia del frontend del docente. */
    public record AlertaCursoResponse(String id, Long alumnoId, String alumnoNombre, Long cursoId, String cursoNombre,
                                      int inasistenciasConsecutivas, int porcentajeActual, String nivelRiesgo,
                                      LocalDate fechaUltimaInasistencia) {
    }
}
