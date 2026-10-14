package pe.edu.vallegrande.app.model.enums;

import java.time.DayOfWeek;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Días lectivos de un horario (lunes a viernes). */
public enum DiaSemana implements ValorTexto {
    LUNES("Lunes", "Lun", DayOfWeek.MONDAY),
    MARTES("Martes", "Mar", DayOfWeek.TUESDAY),
    MIERCOLES("Miércoles", "Mié", DayOfWeek.WEDNESDAY),
    JUEVES("Jueves", "Jue", DayOfWeek.THURSDAY),
    VIERNES("Viernes", "Vie", DayOfWeek.FRIDAY);

    private final String valor;
    private final String abreviatura;
    private final DayOfWeek diaJava;

    DiaSemana(String valor, String abreviatura, DayOfWeek diaJava) {
        this.valor = valor;
        this.abreviatura = abreviatura;
        this.diaJava = diaJava;
    }

    @Override
    @JsonValue
    public String getValor() {
        return valor;
    }

    public String getAbreviatura() {
        return abreviatura;
    }

    public DayOfWeek getDiaJava() {
        return diaJava;
    }

    public static DiaSemana desde(DayOfWeek dia) {
        for (DiaSemana item : values()) {
            if (item.diaJava == dia) {
                return item;
            }
        }
        return null;
    }

    @JsonCreator
    public static DiaSemana de(String texto) {
        return ValorTexto.desde(DiaSemana.class, texto, "día");
    }

    @jakarta.persistence.Converter
    public static class Convertidor extends ConvertidorValor<DiaSemana> {
        public Convertidor() {
            super(DiaSemana.class, "día");
        }
    }
}
