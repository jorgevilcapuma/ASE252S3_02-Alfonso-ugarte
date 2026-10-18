package pe.edu.vallegrande.app.config;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import pe.edu.vallegrande.app.model.enums.ValorTexto;
import pe.edu.vallegrande.app.seguridad.AutenticacionInterceptor;
import pe.edu.vallegrande.app.seguridad.SesionArgumentResolver;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AutenticacionInterceptor autenticacionInterceptor;
    private final PropiedadesApp propiedades;

    public WebConfig(AutenticacionInterceptor autenticacionInterceptor, PropiedadesApp propiedades) {
        this.autenticacionInterceptor = autenticacionInterceptor;
        this.propiedades = propiedades;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(autenticacionInterceptor).addPathPatterns("/api/**");
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new SesionArgumentResolver());
    }

    /** Angular (ng serve en :4200) consume la API desde otro origen. */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(propiedades.corsOrigins())
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Authorization");
    }

    /** Los parámetros de consulta aceptan el mismo texto que el JSON (?estado=Pendiente, ?perfil=docente). */
    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverterFactory(new TextoACatalogo());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static final class TextoACatalogo implements ConverterFactory<String, Enum> {

        @Override
        public <T extends Enum> Converter<String, T> getConverter(Class<T> tipo) {
            return texto -> {
                if (texto == null || texto.isBlank()) {
                    return null;
                }
                if (!ValorTexto.class.isAssignableFrom(tipo)) {
                    return (T) Enum.valueOf(tipo, texto.trim());
                }
                String buscado = ValorTexto.normalizar(texto);
                for (T constante : tipo.getEnumConstants()) {
                    if (ValorTexto.normalizar(((ValorTexto) constante).getValor()).equals(buscado)
                            || constante.name().equalsIgnoreCase(texto.trim())) {
                        return constante;
                    }
                }
                String permitidos = Arrays.stream(tipo.getEnumConstants())
                        .map(constante -> ((ValorTexto) constante).getValor())
                        .collect(Collectors.joining(", "));
                throw new IllegalArgumentException("Valor no válido: '" + texto.trim() + "'. Valores permitidos: " + permitidos + ".");
            };
        }
    }
}
