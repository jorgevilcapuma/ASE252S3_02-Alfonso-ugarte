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
import pe.edu.vallegrande.app.dto.request.CursoRequest;
import pe.edu.vallegrande.app.dto.response.CursoResponse;
import pe.edu.vallegrande.app.model.enums.Perfil;
import pe.edu.vallegrande.app.seguridad.RequierePerfil;
import pe.edu.vallegrande.app.service.CursoService;

@Tag(name = "Cursos (catálogo de áreas)")
@RestController
@RequestMapping("/api/cursos")
@RequiredArgsConstructor
@RequierePerfil(Perfil.ADMINISTRADOR)
public class CursoRest {

    private final CursoService service;

    @RequierePerfil({Perfil.ADMINISTRADOR, Perfil.DOCENTE})
    @GetMapping
    public List<CursoResponse> listar() {
        return service.listar();
    }

    @RequierePerfil({Perfil.ADMINISTRADOR, Perfil.DOCENTE})
    @GetMapping("/{id}")
    public CursoResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CursoResponse crear(@Valid @RequestBody CursoRequest request) {
        return service.crear(request);
    }

    @PutMapping("/{id}")
    public CursoResponse actualizar(@PathVariable Long id, @Valid @RequestBody CursoRequest request) {
        return service.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        service.eliminar(id);
    }
}
