package pe.edu.vallegrande.app.dto.response;

/** El token se envía luego en la cabecera "Authorization: Bearer &lt;token&gt;". */
public record LoginResponse(String token, long expiraEn, SesionResponse sesion) {
}
