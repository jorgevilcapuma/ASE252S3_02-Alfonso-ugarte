package pe.edu.vallegrande.app.util;

import java.util.Locale;

import pe.edu.vallegrande.app.exception.ApiException;

/** Filtro "estado" de los listados con eliminación lógica: A = activos, I = inactivos, T (o vacío) = todos. */
public final class FiltroEstado {

    public static final String ACTIVO = "A";
    public static final String INACTIVO = "I";

    private FiltroEstado() {
    }

    /** "A" o "I" para filtrar; null cuando se piden todos. Acepta también "Activo" e "Inactivo". */
    public static String valor(String estado) {
        String texto = Textos.limpiar(estado);
        if (texto == null) {
            return null;
        }
        return switch (texto.toUpperCase(Locale.ROOT)) {
            case "T", "TODOS" -> null;
            case "A", "ACTIVO", "ACTIVOS" -> ACTIVO;
            case "I", "INACTIVO", "INACTIVOS" -> INACTIVO;
            default -> throw ApiException.validacion("El estado debe ser A (activos), I (inactivos) o T (todos).");
        };
    }
}
