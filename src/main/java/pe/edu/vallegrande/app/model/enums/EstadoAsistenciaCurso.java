package pe.edu.vallegrande.app.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Estado de un alumno en una sesión de clase registrada por el docente. */
public enum EstadoAsistenciaCurso implements ValorTexto {
    ASISTIO("asistio"),
    TARDANZA("tardanza"),
    FALTA("falta"),
    JUSTIFICADO("justificado");

    private final String valor;

    EstadoAsistenciaCurso(String valor) {
        this.valor = valor;
    }

    @Override
    @JsonValue
    public String getValor() {
        return valor;
    }

    @JsonCreator
    public static EstadoAsistenciaCurso de(String texto) {
        return ValorTexto.desde(EstadoAsistenciaCurso.class, texto, "estado de asistencia del curso");
    }

    /** Estado equivalente en la vista del alumno (presente/tardanza/falta/justificado). */
    public EstadoAsistencia comoDiario() {
        return switch (this) {
            case ASISTIO -> EstadoAsistencia.PRESENTE;
            case TARDANZA -> EstadoAsistencia.TARDANZA;
            case FALTA -> EstadoAsistencia.FALTA;
            case JUSTIFICADO -> EstadoAsistencia.JUSTIFICADO;
        };
    }

    @jakarta.persistence.Converter
    public static class Convertidor extends ConvertidorValor<EstadoAsistenciaCurso> {
        public Convertidor() {
            super(EstadoAsistenciaCurso.class, "estado de asistencia del curso");
        }
    }
}
