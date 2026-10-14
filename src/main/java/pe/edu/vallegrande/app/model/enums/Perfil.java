package pe.edu.vallegrande.app.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Perfil de acceso al portal. */
public enum Perfil implements ValorTexto {
    ADMINISTRADOR("administrador"),
    DOCENTE("docente"),
    ALUMNO("alumno");

    private final String valor;

    Perfil(String valor) {
        this.valor = valor;
    }

    @Override
    @JsonValue
    public String getValor() {
        return valor;
    }

    @JsonCreator
    public static Perfil de(String texto) {
        return ValorTexto.desde(Perfil.class, texto, "perfil");
    }

    @jakarta.persistence.Converter
    public static class Convertidor extends ConvertidorValor<Perfil> {
        public Convertidor() {
            super(Perfil.class, "perfil");
        }
    }
}
