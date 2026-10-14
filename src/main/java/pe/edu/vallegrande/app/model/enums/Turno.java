package pe.edu.vallegrande.app.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Turno de funcionamiento de una sección. */
public enum Turno implements ValorTexto {
    MANANA("Mañana"),
    TARDE("Tarde");

    private final String valor;

    Turno(String valor) {
        this.valor = valor;
    }

    @Override
    @JsonValue
    public String getValor() {
        return valor;
    }

    @JsonCreator
    public static Turno de(String texto) {
        return ValorTexto.desde(Turno.class, texto, "turno");
    }

    @jakarta.persistence.Converter
    public static class Convertidor extends ConvertidorValor<Turno> {
        public Convertidor() {
            super(Turno.class, "turno");
        }
    }
}
