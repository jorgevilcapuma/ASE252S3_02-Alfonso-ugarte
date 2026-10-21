package pe.edu.vallegrande.app.util;

/** Expresiones y mensajes de validación compartidos por las solicitudes (DTO) de la API. */
public final class Patrones {

    /** Letras (con tildes y ñ), espacios, apóstrofo, punto o guion; empieza con letra (los espacios extra se limpian luego). */
    public static final String NOMBRE = "^\\s*[A-Za-zÁÉÍÓÚÜÑáéíóúüñ][A-Za-zÁÉÍÓÚÜÑáéíóúüñ' .-]*$";
    public static final String NOMBRE_MENSAJE = "Los nombres y apellidos solo admiten letras, espacios, apóstrofo o guion.";

    /** Vacío o un correo con dominio completo (usuario@dominio.ext). */
    public static final String CORREO = "^$|^[^\\s@]+@[^\\s@]+\\.[A-Za-z]{2,}$";
    public static final String CORREO_MENSAJE = "Ingresa un correo válido, por ejemplo nombre@ie20191.edu.pe.";

    private Patrones() {
    }
}
