package pe.edu.vallegrande.app.model.enums;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Catálogo cerrado cuyo valor en JSON y en la base de datos es el mismo texto que usa el frontend
 * (por ejemplo "presente", "Pendiente", "Mañana").
 */
public interface ValorTexto {

    String getValor();

    /** Busca la constante por su valor (sin distinguir mayúsculas ni tildes) o por su nombre. */
    static <E extends Enum<E> & ValorTexto> E desde(Class<E> tipo, String texto, String campo) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        String buscado = normalizar(texto);
        for (E constante : tipo.getEnumConstants()) {
            if (normalizar(constante.getValor()).equals(buscado) || constante.name().equalsIgnoreCase(texto.trim())) {
                return constante;
            }
        }
        String permitidos = Arrays.stream(tipo.getEnumConstants()).map(ValorTexto::getValor).collect(Collectors.joining(", "));
        throw new IllegalArgumentException("Valor no válido para " + campo + ": '" + texto.trim() + "'. Valores permitidos: " + permitidos + ".");
    }

    static String normalizar(String texto) {
        return Normalizer.normalize(texto.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[\\s_]+", " ")
                .toLowerCase();
    }
}
