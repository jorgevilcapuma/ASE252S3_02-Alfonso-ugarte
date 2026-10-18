package pe.edu.vallegrande.app.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Estado de asistencia de un estudiante (P/T/F/J). "pendiente" significa que no se registró: no se guarda. */
public enum EstadoAsistencia implements ValorTexto {
    PRESENTE("presente"),
    TARDANZA("tardanza"),
    FALTA("falta"),
    JUSTIFICADO("justificado"),
    PENDIENTE("pendiente");

    private final String valor;

    EstadoAsistencia(String valor) {
        this.valor = valor;
    }

    @Override
    @JsonValue
    public String getValor() {
        return valor;
    }

    @JsonCreator
    public static EstadoAsistencia de(String texto) {
        return ValorTexto.desde(EstadoAsistencia.class, texto, "estado de asistencia");
    }

    @jakarta.persistence.Converter
    public static class Convertidor extends ConvertidorValor<EstadoAsistencia> {
        public Convertidor() {
            super(EstadoAsistencia.class, "estado de asistencia");
        }
    }
}
