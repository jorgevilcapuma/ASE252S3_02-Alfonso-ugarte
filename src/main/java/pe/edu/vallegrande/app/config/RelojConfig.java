package pe.edu.vallegrande.app.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Reloj en la zona del colegio: el "hoy" de la asistencia no depende de la zona del servidor. */
@Configuration
public class RelojConfig {

    @Bean
    public Clock reloj(PropiedadesApp propiedades) {
        return Clock.system(ZoneId.of(propiedades.zonaHoraria()));
    }
}
