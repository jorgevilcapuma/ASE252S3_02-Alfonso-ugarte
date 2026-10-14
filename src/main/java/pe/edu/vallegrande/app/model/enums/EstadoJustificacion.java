package pe.edu.vallegrande.app.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Estado de una solicitud de justificación. */
public enum EstadoJustificacion implements ValorTexto {
    PENDIENTE("Pendiente"),
    APROBADA("Aprobada"),
    OBSERVADA("Observada");

    private final String valor;

    EstadoJustificacion(String valor) {
        this.valor = valor;
    }

    @Override
    @JsonValue
    public String getValor() {
        return valor;
    }

    @JsonCreator
    public static EstadoJustificacion de(String texto) {
        return ValorTexto.desde(EstadoJustificacion.class, texto, "estado de la justificación");
    }

    @jakarta.persistence.Converter
    public static class Convertidor extends ConvertidorValor<EstadoJustificacion> {
        public Convertidor() {
            super(EstadoJustificacion.class, "estado de la justificación");
        }
    }
}
