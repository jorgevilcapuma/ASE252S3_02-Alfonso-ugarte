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
                        .description("""
                                Control de asistencia de estudiantes, docentes y personal administrativo, con justificaciones, \
                                de la I.E. 20191 Alfonso Ugarte.

                                **Cómo probar (si no, todo responde 401):**
                                1. Abre *Autenticación → POST /api/auth/login*, pulsa **Try it out** y envía
                                   `{"perfil": "administrador", "correo": "admin@ie20191.edu.pe", "contrasena": "123456"}`.
                                2. Copia el valor de `token` de la respuesta (sin comillas).
                                3. Pulsa el botón **Authorize** (arriba a la derecha), pega el token y confirma.

                                El token se recuerda aunque recargues la página y dura 10 horas."""))
                .components(new Components().addSecuritySchemes("token",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer")))
                .addSecurityItem(new SecurityRequirement().addList("token"));
    }
}
