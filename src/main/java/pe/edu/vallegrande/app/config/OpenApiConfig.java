package pe.edu.vallegrande.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

/** Swagger en /swagger-ui.html. Usa "Authorize" con el token devuelto por POST /api/auth/login. */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Portal Ugartino Digital - API")
                        .version("1.0")
                        .description("Asistencia escolar, justificaciones, alertas tempranas y portales por perfil de la I.E. 20191 Alfonso Ugarte."))
                .components(new Components().addSecuritySchemes("token",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer")))
                .addSecurityItem(new SecurityRequirement().addList("token"));
    }
}
