package pe.edu.vallegrande.app.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Estado de una cuenta institucional. */
public enum EstadoUsuario implements ValorTexto {
    ACTIVO("Activo"),
    INACTIVO("Inactivo");

    private final String valor;

    EstadoUsuario(String valor) {
        this.valor = valor;
    }

    @Override
    @JsonValue
    public String getValor() {
        return valor;
    }

    @JsonCreator
    public static EstadoUsuario de(String texto) {
        return ValorTexto.desde(EstadoUsuario.class, texto, "estado");
    }

    @jakarta.persistence.Converter
    public static class Convertidor extends ConvertidorValor<EstadoUsuario> {
        public Convertidor() {
            super(EstadoUsuario.class, "estado");
        }
    }
}
