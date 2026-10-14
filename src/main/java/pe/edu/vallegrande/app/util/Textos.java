package pe.edu.vallegrande.app.util;

import java.text.Normalizer;

/** Normalización de textos ingresados en formularios. */
public final class Textos {

    private Textos() {
    }

    /** Recorta y colapsa espacios; devuelve null si queda vacío. */
    public static String limpiar(String texto) {
        if (texto == null) {
            return null;
        }
        String limpio = texto.trim().replaceAll("\\s+", " ");
        return limpio.isEmpty() ? null : limpio;
    }

    /** Para comparar nombres sin importar tildes, mayúsculas ni espacios repetidos. */
    public static String clave(String texto) {
        if (texto == null) {
            return "";
        }
        return Normalizer.normalize(texto.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("\\s+", " ")
                .toLowerCase();
    }

    /**
     * Separa "Nombres Apellido1 Apellido2": con 3 o más palabras los dos últimos son apellidos,
     * con 2 palabras el último. Devuelve null si hay menos de dos palabras.
     */
    public static String[] separarNombreCompleto(String nombreCompleto) {
        String limpio = limpiar(nombreCompleto);
        if (limpio == null) {
            return null;
        }
        String[] partes = limpio.split(" ");
        if (partes.length < 2) {
            return null;
        }
        int corte = partes.length >= 3 ? partes.length - 2 : 1;
        return new String[]{
                String.join(" ", java.util.Arrays.copyOfRange(partes, 0, corte)),
                String.join(" ", java.util.Arrays.copyOfRange(partes, corte, partes.length))
        };
    }

    public static String correo(String correo) {
        String limpio = limpiar(correo);
        return limpio == null ? null : limpio.toLowerCase();
    }
}
