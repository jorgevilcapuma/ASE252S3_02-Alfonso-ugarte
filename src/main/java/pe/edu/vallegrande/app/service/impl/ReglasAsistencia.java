package pe.edu.vallegrande.app.service.impl;

import java.time.LocalDate;

import org.springframework.stereotype.Component;

import pe.edu.vallegrande.app.config.PropiedadesApp;
import pe.edu.vallegrande.app.exception.ApiException;
import pe.edu.vallegrande.app.seguridad.SesionUsuario;
import pe.edu.vallegrande.app.util.Calendario;

/** Reglas de asistencia del colegio (application.yml, sección app.asistencia), compartidas por los servicios. */
@Component
class ReglasAsistencia {

    private final PropiedadesApp propiedades;
    private final Calendario calendario;

    ReglasAsistencia(PropiedadesApp propiedades, Calendario calendario) {
        this.propiedades = propiedades;
        this.calendario = calendario;
    }

    PropiedadesApp.Asistencia vigentes() {
        return propiedades.asistencia();
    }

    /**
     * Un docente solo corrige asistencia de hoy y de los últimos {@code diasEdicionDocente} días hábiles;
     * fechas anteriores quedan cerradas y solo el administrador puede modificarlas.
     */
    void validarPlazoDocente(LocalDate fecha, SesionUsuario sesion) {
        if (!sesion.esDocente()) {
            return;
        }
        int dias = vigentes().diasEdicionDocente();
        LocalDate limite = Calendario.diasHabiles(dias + 1, calendario.hoy()).get(0);
        if (fecha.isBefore(limite)) {
            throw ApiException.prohibido("La asistencia del " + fecha + " ya está cerrada: los docentes pueden corregir hasta "
                    + dias + " días hábiles atrás. Solicita el cambio al administrador.");
        }
    }
}
