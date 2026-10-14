package pe.edu.vallegrande.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Parámetros del sistema definidos en application.yml (sección "app"). */
@ConfigurationProperties(prefix = "app")
public record PropiedadesApp(
        String zonaHoraria,
        String[] corsOrigins,
        Seguridad seguridad,
        Alertas alertas,
        DatosDemo datosDemo) {

    public record Seguridad(String secreto, int horasSesion, String contrasenaInicial) {
    }

    /** Reglas de alertas tempranas: faltas acumuladas en los últimos {@code ventanaDias} días hábiles. */
    public record Alertas(int ventanaDias, int faltasCriticas, int faltasPreventivas) {
    }

    public record DatosDemo(boolean habilitado, int diasHistorial) {
    }
}
