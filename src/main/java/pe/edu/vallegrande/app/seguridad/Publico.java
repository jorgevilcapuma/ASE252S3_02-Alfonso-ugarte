package pe.edu.vallegrande.app.seguridad;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Endpoint accesible sin sesión (login, datos de la institución para la pantalla de ingreso). */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Publico {
}
