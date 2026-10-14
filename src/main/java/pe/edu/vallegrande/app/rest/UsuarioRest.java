package pe.edu.vallegrande.app.rest;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import pe.edu.vallegrande.app.dto.request.CambioContrasenaRequest;
import pe.edu.vallegrande.app.dto.request.UsuarioRequest;
import pe.edu.vallegrande.app.dto.response.UsuarioResponse;
import pe.edu.vallegrande.app.model.enums.EstadoUsuario;
import pe.edu.vallegrande.app.model.enums.Perfil;
import pe.edu.vallegrande.app.seguridad.RequierePerfil;
import pe.edu.vallegrande.app.seguridad.SesionUsuario;
import pe.edu.vallegrande.app.service.UsuarioService;

@Tag(name = "Gestión de usuarios")
@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
@RequierePerfil(Perfil.ADMINISTRADOR)
public class UsuarioRest {

    private final UsuarioService service;

    @Operation(summary = "Listar cuentas (filtros: perfil, estado, texto en nombre o correo)")
    @GetMapping
    public List<UsuarioResponse> listar(@RequestParam(required = false) Perfil perfil, @RequestParam(required = false) EstadoUsuario estado,
                                        @RequestParam(required = false) String q) {
        return service.listar(perfil, estado, q);
    }

    @GetMapping("/{id}")
    public UsuarioResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse crear(@Valid @RequestBody UsuarioRequest request) {
        return service.crear(request);
    }

    @PutMapping("/{id}")
    public UsuarioResponse actualizar(@PathVariable Long id, @Valid @RequestBody UsuarioRequest request, SesionUsuario sesion) {
        return service.actualizar(id, request, sesion);
    }

    @Operation(summary = "Activar / desactivar la cuenta")
    @PatchMapping("/{id}/estado")
    public UsuarioResponse alternarEstado(@PathVariable Long id, SesionUsuario sesion) {
        return service.alternarEstado(id, sesion);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id, SesionUsuario sesion) {
        service.eliminar(id, sesion);
    }

    @Operation(summary = "Cambiar contraseña (propia con la actual, o cualquiera siendo administrador)")
    @RequierePerfil({Perfil.ADMINISTRADOR, Perfil.DOCENTE, Perfil.ALUMNO})
    @PutMapping("/{id}/contrasena")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cambiarContrasena(@PathVariable Long id, @Valid @RequestBody CambioContrasenaRequest request, SesionUsuario sesion) {
        service.cambiarContrasena(id, request, sesion);
    }
}
