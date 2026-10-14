package pe.edu.vallegrande.app.service;

import pe.edu.vallegrande.app.dto.request.LoginRequest;
import pe.edu.vallegrande.app.dto.response.LoginResponse;
import pe.edu.vallegrande.app.dto.response.SesionResponse;
import pe.edu.vallegrande.app.seguridad.SesionUsuario;

public interface AuthService {

    LoginResponse ingresar(LoginRequest request);

    SesionResponse sesion(SesionUsuario sesion);
}
