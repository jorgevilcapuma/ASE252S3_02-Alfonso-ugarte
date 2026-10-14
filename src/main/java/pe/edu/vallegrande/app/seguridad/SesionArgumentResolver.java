package pe.edu.vallegrande.app.seguridad;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import pe.edu.vallegrande.app.exception.ApiException;

/** Permite recibir {@link SesionUsuario} como parámetro en los controladores. */
public class SesionArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return SesionUsuario.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        Object sesion = webRequest.getAttribute(AutenticacionInterceptor.ATRIBUTO_SESION, RequestAttributes.SCOPE_REQUEST);
        if (sesion == null) {
            throw ApiException.noAutorizado("Inicia sesión para continuar.");
        }
        return sesion;
    }
}
