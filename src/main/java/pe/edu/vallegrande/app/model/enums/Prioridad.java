package pe.edu.vallegrande.app.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Prioridad de un comunicado. */
public enum Prioridad implements ValorTexto {
    ALTA("Alta"),
    MEDIA("Media"),
    BAJA("Baja");

    private final String valor;

    Prioridad(String valor) {
        this.valor = valor;
    }

    @Override
    @JsonValue
    public String getValor() {
        return valor;
    }

    @JsonCreator
    public static Prioridad de(String texto) {
        return ValorTexto.desde(Prioridad.class, texto, "prioridad");
    }

    @jakarta.persistence.Converter
    public static class Convertidor extends ConvertidorValor<Prioridad> {
        public Convertidor() {
            super(Prioridad.class, "prioridad");
        }
    }
}
