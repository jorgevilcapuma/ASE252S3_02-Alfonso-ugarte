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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import pe.edu.vallegrande.app.dto.request.FaseRequest;
import pe.edu.vallegrande.app.dto.response.FaseResponse;
import pe.edu.vallegrande.app.model.enums.Perfil;
import pe.edu.vallegrande.app.seguridad.RequierePerfil;
import pe.edu.vallegrande.app.service.FaseService;

@Tag(name = "Fases del proyecto")
@RestController
@RequestMapping("/api/fases")
@RequiredArgsConstructor
@RequierePerfil(Perfil.ADMINISTRADOR)
public class FaseRest {

    private final FaseService service;

    @RequierePerfil({Perfil.ADMINISTRADOR, Perfil.DOCENTE, Perfil.ALUMNO})
    @GetMapping
    public List<FaseResponse> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public FaseResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FaseResponse crear(@Valid @RequestBody FaseRequest request) {
        return service.crear(request);
    }

    @PutMapping("/{id}")
    public FaseResponse actualizar(@PathVariable Long id, @Valid @RequestBody FaseRequest request) {
        return service.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        service.eliminar(id);
    }
}
