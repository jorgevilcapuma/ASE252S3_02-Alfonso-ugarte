package pe.edu.vallegrande.app.rest;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import pe.edu.vallegrande.app.dto.request.LoginRequest;
import pe.edu.vallegrande.app.dto.response.LoginResponse;
import pe.edu.vallegrande.app.dto.response.SesionResponse;
import pe.edu.vallegrande.app.seguridad.Publico;
import pe.edu.vallegrande.app.seguridad.SesionUsuario;
import pe.edu.vallegrande.app.service.AuthService;

@Tag(name = "Autenticación")
@RestController
@RequestMapping("/api/auth")
public class AuthRest {

    private final AuthService service;

    public AuthRest(AuthService service) {
        this.service = service;
    }

    @Operation(summary = "Ingresar al portal",
            description = "Administrador/docente: correo y contraseña. Alumno: nombre completo y DNI. Devuelve el token Bearer.")
    @Publico
    @PostMapping("/login")
    public LoginResponse ingresar(@Valid @RequestBody LoginRequest request) {
        return service.ingresar(request);
    }

    @Operation(summary = "Datos de la sesión actual")
    @GetMapping("/sesion")
    public SesionResponse sesion(SesionUsuario sesion) {
        return service.sesion(sesion);
    }
}
