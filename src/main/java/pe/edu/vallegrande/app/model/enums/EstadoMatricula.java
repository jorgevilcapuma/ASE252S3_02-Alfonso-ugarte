package pe.edu.vallegrande.app.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Estado de la matrícula del año académico. Retirar conserva el historial de asistencia. */
public enum EstadoMatricula implements ValorTexto {
    ACTIVA("Activa"),
    RETIRADA("Retirada");

    private final String valor;

    EstadoMatricula(String valor) {
        this.valor = valor;
    }

    @Override
    @JsonValue
    public String getValor() {
        return valor;
    }

    @JsonCreator
    public static EstadoMatricula de(String texto) {
        return ValorTexto.desde(EstadoMatricula.class, texto, "estado de la matrícula");
    }

    @jakarta.persistence.Converter
    public static class Convertidor extends ConvertidorValor<EstadoMatricula> {
        public Convertidor() {
            super(EstadoMatricula.class, "estado de la matrícula");
        }
    }
}
