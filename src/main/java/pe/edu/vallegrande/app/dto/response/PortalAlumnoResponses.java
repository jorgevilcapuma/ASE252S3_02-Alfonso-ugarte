package pe.edu.vallegrande.app.dto.response;

import java.time.LocalDate;

import pe.edu.vallegrande.app.model.enums.EstadoAsistencia;

/** Respuestas del portal del alumno. */
public final class PortalAlumnoResponses {

    private PortalAlumnoResponses() {
    }

    /** Interfaz DiaAlumno: un día (y curso, si aplica) del historial del alumno. */
    public record DiaAlumnoResponse(LocalDate fecha, EstadoAsistencia estado, Long cursoId, String cursoNombre, String docente,
                                    String hora, String observacion) {
    }

    public record ResumenAsistenciaResponse(int asistencias, int tardanzas, int faltas, int justificadas, int total,
                                            int porcentajeAsistencia, int porcentajeInasistencia) {
    }

    /** Interfaz AvisoAlumno. tipo: asistencia, justificacion o comunicado. */
    public record AvisoAlumnoResponse(String id, LocalDate fecha, String titulo, String detalle, String tipo, boolean leido) {
    }

    /** Pantalla de inicio del alumno. */
    public record InicioAlumnoResponse(EstudianteResponse estudiante, String institucion, LocalDate hoy, EstadoAsistencia estadoHoy,
                                       ResumenAsistenciaResponse resumen, long justificacionesPendientes, int cursos) {
    }
}
