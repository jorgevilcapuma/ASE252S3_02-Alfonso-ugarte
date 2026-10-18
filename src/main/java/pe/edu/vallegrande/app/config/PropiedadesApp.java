package pe.edu.vallegrande.app.config;

import java.time.LocalTime;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Parámetros del sistema definidos en application.yml (sección "app"). */
@ConfigurationProperties(prefix = "app")
public record PropiedadesApp(
        String zonaHoraria,
        String[] corsOrigins,
        Seguridad seguridad,
        Asistencia asistencia,
        DatosDemo datosDemo) {

    public record Seguridad(String secreto, int horasSesion, String contrasenaInicial) {
    }

    /**
     * Reglas de asistencia del colegio.
     *
     * @param horaEntrada        hora oficial de ingreso del personal
     * @param minutosTolerancia  después de horaEntrada + tolerancia la marcación es tardanza
     * @param diasEdicionDocente días hábiles anteriores a hoy en los que un docente aún puede corregir
     *                           asistencia de estudiantes; después, solo el administrador
     */
    public record Asistencia(LocalTime horaEntrada, int minutosTolerancia, int diasEdicionDocente) {

        public LocalTime limitePuntualidad() {
            return horaEntrada.plusMinutes(minutosTolerancia);
        }
    }

    public record DatosDemo(boolean habilitado, int diasHistorial) {
    }
}
