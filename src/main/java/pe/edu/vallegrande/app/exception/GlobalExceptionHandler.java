package pe.edu.vallegrande.app.exception;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/** Convierte cualquier error en { status, mensaje, errores[], ruta, fecha } con un código HTTP coherente. */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> api(ApiException ex, HttpServletRequest request) {
        return responder(ex.getStatus(), ex.getMessage(), ex.getErrores(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validacion(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<String> errores = ex.getBindingResult().getAllErrors().stream()
                .map(error -> error.getDefaultMessage())
                .distinct()
                .toList();
        return responder(HttpStatus.BAD_REQUEST, "Revisa los datos enviados.", errores, request);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> validacionParametros(HandlerMethodValidationException ex, HttpServletRequest request) {
        List<String> errores = ex.getAllValidationResults().stream()
                .flatMap(resultado -> resultado.getResolvableErrors().stream())
                .map(error -> error.getDefaultMessage())
                .distinct()
                .toList();
        return responder(HttpStatus.BAD_REQUEST, "Revisa los parámetros enviados.", errores, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> cuerpoInvalido(HttpMessageNotReadableException ex, HttpServletRequest request) {
        // Un valor de catálogo desconocido (estado, perfil, prioridad...) llega como IllegalArgumentException con mensaje propio.
        Throwable causa = ex;
        while (causa != null) {
            if (causa instanceof IllegalArgumentException && causa.getMessage() != null) {
                return responder(HttpStatus.BAD_REQUEST, causa.getMessage(), null, request);
            }
            causa = causa.getCause();
        }
        return responder(HttpStatus.BAD_REQUEST, "El cuerpo de la solicitud no tiene el formato JSON esperado.", null, request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> tipoInvalido(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        Throwable raiz = ex.getMostSpecificCause();
        String mensaje = raiz instanceof IllegalArgumentException && raiz.getMessage() != null && !raiz.getMessage().startsWith("For input")
                ? raiz.getMessage()
                : "El parámetro '" + ex.getName() + "' no tiene un formato válido.";
        return responder(HttpStatus.BAD_REQUEST, mensaje, null, request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> faltaParametro(MissingServletRequestParameterException ex, HttpServletRequest request) {
        return responder(HttpStatus.BAD_REQUEST, "Falta el parámetro '" + ex.getParameterName() + "'.", null, request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> integridad(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Restricción de base de datos violada: {}", ex.getMostSpecificCause().getMessage());
        return responder(HttpStatus.CONFLICT, "La operación entra en conflicto con datos existentes (registro duplicado o en uso).", null, request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> rutaInexistente(NoResourceFoundException ex, HttpServletRequest request) {
        return responder(HttpStatus.NOT_FOUND, "La ruta solicitada no existe.", null, request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> metodo(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        return responder(HttpStatus.METHOD_NOT_ALLOWED, "El método " + ex.getMethod() + " no está permitido en esta ruta.", null, request);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> tipoContenido(HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
        return responder(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Envía el cuerpo como application/json.", null, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> inesperado(Exception ex, HttpServletRequest request) {
        log.error("Error no controlado en {}", request.getRequestURI(), ex);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error inesperado. Intenta nuevamente.", null, request);
    }

    private static ResponseEntity<ErrorResponse> responder(HttpStatus status, String mensaje, List<String> errores, HttpServletRequest request) {
        return ResponseEntity.status(status).body(ErrorResponse.de(status.value(), mensaje, errores, request.getRequestURI()));
    }
}
