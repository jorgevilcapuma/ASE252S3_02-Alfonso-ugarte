package pe.edu.vallegrande.app.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Nivel educativo de un aula. */
public enum Nivel implements ValorTexto {
    INICIAL("Inicial"),
    PRIMARIA("Primaria"),
    SECUNDARIA("Secundaria");

    private final String valor;

    Nivel(String valor) {
        this.valor = valor;
    }

    @Override
    @JsonValue
    public String getValor() {
        return valor;
    }

    @JsonCreator
    public static Nivel de(String texto) {
        return ValorTexto.desde(Nivel.class, texto, "nivel");
    }

    @jakarta.persistence.Converter
    public static class Convertidor extends ConvertidorValor<Nivel> {
        public Convertidor() {
            super(Nivel.class, "nivel");
        }
    }
}
