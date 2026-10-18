package pe.edu.vallegrande.app.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

import pe.edu.vallegrande.app.model.Justificacion;
import pe.edu.vallegrande.app.model.enums.EstadoAsistencia;
import pe.edu.vallegrande.app.model.enums.EstadoJustificacion;

/** Solicitud de justificación (cursoId null = día completo; observacion = respuesta de quien revisó). */
public record JustificacionResponse(
        Long id,
        Long estudianteId,
        String estudianteNombre,
        String estudianteDni,
        LocalDate fecha,
        Long cursoId,
        String cursoNombre,
        String motivo,
        String detalle,
        String archivoNombre,
        EstadoJustificacion estado,
        EstadoAsistencia estadoPrevio,
        String observacion,
        LocalDateTime fechaSolicitud,
        LocalDateTime fechaRevision,
        String revisadoPor) {

    public static JustificacionResponse de(Justificacion j) {
        return new JustificacionResponse(j.getId(), j.getEstudiante().getId(), j.getEstudiante().getNombreCompleto(),
                j.getEstudiante().getDni(), j.getFecha(),
                j.getCurso() == null ? null : j.getCurso().getId(),
                j.getCurso() == null ? null : j.getCurso().getNombre(),
                j.getMotivo(), j.getDetalle(), j.getArchivoNombre(), j.getEstado(), j.getEstadoPrevio(), j.getObservacion(),
                j.getFechaSolicitud(), j.getFechaRevision(),
                j.getRevisadoPor() == null ? null : j.getRevisadoPor().getNombre());
    }
}
