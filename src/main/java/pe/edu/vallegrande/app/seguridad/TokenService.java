package pe.edu.vallegrande.app.seguridad;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import pe.edu.vallegrande.app.config.PropiedadesApp;
import pe.edu.vallegrande.app.exception.ApiException;

/** Token de sesión firmado con HMAC-SHA256: base64url(json) + "." + base64url(firma). */
@Component
public class TokenService {

    private static final Base64.Encoder CODIFICADOR = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODIFICADOR = Base64.getUrlDecoder();

    private final ObjectMapper mapper;
    private final Clock reloj;
    private final byte[] secreto;
    private final Duration duracion;

    public TokenService(ObjectMapper mapper, Clock reloj, PropiedadesApp propiedades) {
        this.mapper = mapper;
        this.reloj = reloj;
        this.secreto = propiedades.seguridad().secreto().getBytes(StandardCharsets.UTF_8);
        this.duracion = Duration.ofHours(propiedades.seguridad().horasSesion());
    }

    public long calcularExpiracion() {
        return reloj.millis() + duracion.toMillis();
    }

    public String generar(SesionUsuario sesion) {
        try {
            String cuerpo = CODIFICADOR.encodeToString(mapper.writeValueAsBytes(sesion));
            return cuerpo + "." + firmar(cuerpo);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("No se pudo generar el token", ex);
        }
    }

    public SesionUsuario validar(String token) {
        String[] partes = token == null ? new String[0] : token.split("\\.");
        if (partes.length != 2 || !MessageDigest.isEqual(firmar(partes[0]).getBytes(StandardCharsets.UTF_8), partes[1].getBytes(StandardCharsets.UTF_8))) {
            throw ApiException.noAutorizado("La sesión no es válida. Inicia sesión nuevamente.");
        }
        SesionUsuario sesion;
        try {
            sesion = mapper.readValue(DECODIFICADOR.decode(partes[0]), SesionUsuario.class);
        } catch (Exception ex) {
            throw ApiException.noAutorizado("La sesión no es válida. Inicia sesión nuevamente.");
        }
        if (sesion.expiraEn() < reloj.millis()) {
            throw ApiException.noAutorizado("La sesión expiró. Inicia sesión nuevamente.");
        }
        return sesion;
    }

    private String firmar(String cuerpo) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secreto, "HmacSHA256"));
            return CODIFICADOR.encodeToString(mac.doFinal(cuerpo.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("HMAC no disponible", ex);
        }
    }
}
