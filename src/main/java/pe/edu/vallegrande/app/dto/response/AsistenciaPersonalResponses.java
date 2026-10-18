package pe.edu.vallegrande.app.dto.response;

import java.time.LocalDate;
import java.util.List;

/** Respuestas del control de entrada y salida del personal (horas como texto HH:mm). */
public final class AsistenciaPersonalResponses {

    private AsistenciaPersonalResponses() {
    }

    /** estado: puntual, tardanza o sin-marcar. */
    public record FilaPersonal(
            Long docenteId,
            String nombreCompleto,
            String dni,
            String cargo,
            String area,
            String horaEntrada,
            String horaSalida,
            String estado,
            int minutosTarde,
            String observacion) {
    }

    public record ResumenDia(int total, int puntuales, int tardanzas, int sinMarcar, int conSalida) {
    }

    public record AsistenciaPersonalDiaResponse(
            LocalDate fecha,
            String horaEntrada,
            int minutosTolerancia,
            String limitePuntualidad,
            ResumenDia resumen,
            List<FilaPersonal> filas) {
    }

    /** Reporte del mes por persona; diasSinMarcar cuenta los días hábiles transcurridos sin entrada. */
    public record FilaReporte(
            Long docenteId,
            String nombreCompleto,
            String cargo,
            int diasHabiles,
            int diasAsistidos,
            int tardanzas,
            int minutosTardeAcumulados,
            int diasSinMarcar,
            int porcentajeAsistencia) {
    }

    public record ReportePersonalResponse(String mes, int diasHabiles, List<FilaReporte> filas) {
    }
}
