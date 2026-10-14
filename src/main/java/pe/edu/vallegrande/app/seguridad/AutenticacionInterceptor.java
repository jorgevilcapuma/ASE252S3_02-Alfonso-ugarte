package pe.edu.vallegrande.app.seguridad;

import java.util.Arrays;

import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import pe.edu.vallegrande.app.exception.ApiException;

/**
 * Exige "Authorization: Bearer &lt;token&gt;" en /api/** (salvo endpoints {@link Publico})
 * y comprueba el perfil declarado con {@link RequierePerfil}.
 */
@Component
@RequiredArgsConstructor
public class AutenticacionInterceptor implements HandlerInterceptor {

    public static final String ATRIBUTO_SESION = "sesionUsuario";

    private final TokenService tokenService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod metodo) || "OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        if (metodo.hasMethodAnnotation(Publico.class) || metodo.getBeanType().isAnnotationPresent(Publico.class)) {
            return true;
        }

        String cabecera = request.getHeader("Authorization");
        if (cabecera == null || !cabecera.regionMatches(true, 0, "Bearer ", 0, 7)) {
            throw ApiException.noAutorizado("Inicia sesión para continuar.");
        }
        SesionUsuario sesion = tokenService.validar(cabecera.substring(7).trim());

        RequierePerfil requerido = metodo.getMethodAnnotation(RequierePerfil.class);
        if (requerido == null) {
            requerido = metodo.getBeanType().getAnnotation(RequierePerfil.class);
        }
        if (requerido != null && Arrays.stream(requerido.value()).noneMatch(perfil -> perfil == sesion.perfil())) {
            throw ApiException.prohibido("Tu perfil no tiene acceso a esta operación.");
        }

        request.setAttribute(ATRIBUTO_SESION, sesion);
        return true;
    }
}
