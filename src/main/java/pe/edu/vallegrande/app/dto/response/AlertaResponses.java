package pe.edu.vallegrande.app.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Respuestas de alertas tempranas y avisos a familias. */
public final class AlertaResponses {

    private AlertaResponses() {
    }

    /** Interfaz ResumenAlerta del frontend. nivel: critica, preventiva u hoy. */
    public record ResumenAlertaResponse(EstudianteResponse estudiante, int faltas, boolean faltaHoy, String nivel,
                                        LocalDateTime ultimoAviso) {
    }

    public record FaltasDia(LocalDate fecha, long faltas) {
    }

    public record ParametrosAlerta(int ventanaDias, int faltasCriticas, int faltasPreventivas, LocalDate desde, LocalDate hasta) {
    }

    public record AlertasResponse(ParametrosAlerta parametros, int criticas, int preventivas, int faltasHoy,
                                  List<ResumenAlertaResponse> alertas) {
    }

    public record AvisoResponse(Long id, Long estudianteId, String estudianteNombre, LocalDateTime enviadoEn, int faltas,
                                String enviadoPor) {
    }
}
