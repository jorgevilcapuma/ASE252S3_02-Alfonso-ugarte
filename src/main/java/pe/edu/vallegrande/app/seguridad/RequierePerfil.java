package pe.edu.vallegrande.app.seguridad;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import pe.edu.vallegrande.app.model.enums.Perfil;

/** Perfiles que pueden usar el endpoint. En el método reemplaza a la del controlador. */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequierePerfil {

    Perfil[] value();
}
