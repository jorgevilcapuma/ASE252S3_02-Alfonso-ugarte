package pe.edu.vallegrande.app.dto.response;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import pe.edu.vallegrande.app.model.GradoSeccion;
import pe.edu.vallegrande.app.model.enums.EstadoAsistencia;
import pe.edu.vallegrande.app.model.enums.Turno;

/** Respuestas de la asistencia de estudiantes (diaria por aula o por curso). */
public final class AsistenciaResponses {

    private AsistenciaResponses() {
    }

    public record AulaResponse(Long gradoSeccionId, String nivel, String grado, String gradoEtiqueta, String seccion, Turno turno) {

        public static AulaResponse de(GradoSeccion g) {
            return new AulaResponse(g.getId(), g.getNivel().getValor(), g.getGrado(), g.getGradoEtiqueta(), g.getSeccion(), g.getTurno());
        }
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

    /** Asistencia del aula en una fecha; cursoId/cursoNombre son null en la asistencia diaria. */
    public record AsistenciaDiaResponse(AulaResponse aula, Long cursoId, String cursoNombre, LocalDate fecha,
                                        List<RegistroEstudiante> registros, ConteoEstados resumen) {
    }

    public record RegistroEstudiante(Long estudianteId, String nombre, String dni, EstadoAsistencia estado, String observacion) {
    }

    /** Vista mensual: un mapa fecha → estado por estudiante; los fines de semana se informan aparte. */
    public record MatrizMensualResponse(AulaResponse aula, Long cursoId, String cursoNombre, String mes, List<LocalDate> dias,
                                        List<LocalDate> finesDeSemana, List<FilaMensual> filas) {
    }

    public record FilaMensual(Long estudianteId, String nombre, String dni, Map<LocalDate, EstadoAsistencia> estados,
                              ConteoEstados resumen) {
    }

    /** Registro del historial de un estudiante (cursoId null = asistencia diaria). */
    public record RegistroHistorial(LocalDate fecha, Long cursoId, String cursoNombre, EstadoAsistencia estado, String observacion) {
    }
}
