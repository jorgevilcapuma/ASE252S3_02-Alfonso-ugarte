package pe.edu.vallegrande.app.rest;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import pe.edu.vallegrande.app.dto.request.ComunicadoRequest;
import pe.edu.vallegrande.app.dto.response.ComunicadoResponse;
import pe.edu.vallegrande.app.model.enums.Perfil;
import pe.edu.vallegrande.app.seguridad.RequierePerfil;
import pe.edu.vallegrande.app.seguridad.SesionUsuario;
import pe.edu.vallegrande.app.service.ComunicadoService;

@Tag(name = "Comunicados")
@RestController
@RequestMapping("/api/comunicados")
@RequiredArgsConstructor
@RequierePerfil({Perfil.ADMINISTRADOR, Perfil.DOCENTE})
public class ComunicadoRest {

    private final ComunicadoService service;

    @RequierePerfil({Perfil.ADMINISTRADOR, Perfil.DOCENTE, Perfil.ALUMNO})
    @GetMapping
    public List<ComunicadoResponse> listar(@RequestParam(required = false) Long cursoId, SesionUsuario sesion) {
        return service.listar(cursoId, sesion);
    }

    @GetMapping("/{id}")
    public ComunicadoResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ComunicadoResponse crear(@Valid @RequestBody ComunicadoRequest request, SesionUsuario sesion) {
        return service.crear(request, sesion);
    }

    @PutMapping("/{id}")
    public ComunicadoResponse actualizar(@PathVariable Long id, @Valid @RequestBody ComunicadoRequest request, SesionUsuario sesion) {
        return service.actualizar(id, request, sesion);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id, SesionUsuario sesion) {
        service.eliminar(id, sesion);
    }
}
