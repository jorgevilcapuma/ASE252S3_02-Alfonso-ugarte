package pe.edu.vallegrande.app.seguridad;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.springframework.stereotype.Component;

/** Contraseñas con PBKDF2-HMAC-SHA256 y sal aleatoria. Formato: pbkdf2$iteraciones$sal$hash. */
@Component
public class HasherContrasena {

    private static final int ITERACIONES = 65_536;
    private static final int LONGITUD_BITS = 256;
    private final SecureRandom aleatorio = new SecureRandom();

    public String generar(String contrasena) {
        byte[] sal = new byte[16];
        aleatorio.nextBytes(sal);
        byte[] hash = derivar(contrasena, sal, ITERACIONES);
        Base64.Encoder b64 = Base64.getEncoder();
        return "pbkdf2$" + ITERACIONES + "$" + b64.encodeToString(sal) + "$" + b64.encodeToString(hash);
    }

    public boolean coincide(String contrasena, String guardado) {
        if (contrasena == null || guardado == null) {
            return false;
        }
        String[] partes = guardado.split("\\$");
        if (partes.length != 4 || !"pbkdf2".equals(partes[0])) {
            return false;
        }
        Base64.Decoder b64 = Base64.getDecoder();
        byte[] esperado = b64.decode(partes[3]);
        byte[] calculado = derivar(contrasena, b64.decode(partes[2]), Integer.parseInt(partes[1]));
        return MessageDigest.isEqual(esperado, calculado);
    }

    private static byte[] derivar(String contrasena, byte[] sal, int iteraciones) {
        try {
            PBEKeySpec spec = new PBEKeySpec(contrasena.toCharArray(), sal, iteraciones, LONGITUD_BITS);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("PBKDF2 no disponible", ex);
        }
    }
}
