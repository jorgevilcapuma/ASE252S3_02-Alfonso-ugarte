package pe.edu.vallegrande.app.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Avance de una fase del proyecto. */
public enum EstadoFase implements ValorTexto {
    COMPLETADA("Completada"),
    EN_CURSO("En curso"),
    PENDIENTE("Pendiente");

    private final String valor;

    EstadoFase(String valor) {
        this.valor = valor;
    }

    @Override
    @JsonValue
    public String getValor() {
        return valor;
    }

    @JsonCreator
    public static EstadoFase de(String texto) {
        return ValorTexto.desde(EstadoFase.class, texto, "estado de la fase");
    }

    @jakarta.persistence.Converter
    public static class Convertidor extends ConvertidorValor<EstadoFase> {
        public Convertidor() {
            super(EstadoFase.class, "estado de la fase");
        }
    }
}
