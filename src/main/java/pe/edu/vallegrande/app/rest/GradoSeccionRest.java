package pe.edu.vallegrande.app.rest;

import java.util.List;

import org.springframework.http.HttpStatus;
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
import pe.edu.vallegrande.app.dto.request.GradoSeccionRequest;
import pe.edu.vallegrande.app.dto.response.GradoSeccionResponse;
import pe.edu.vallegrande.app.model.enums.Nivel;
import pe.edu.vallegrande.app.model.enums.Perfil;
import pe.edu.vallegrande.app.seguridad.RequierePerfil;
import pe.edu.vallegrande.app.service.GradoSeccionService;

/** Aulas: nivel + grado + sección con su turno. */
@Tag(name = "Grados y secciones")
@RestController
@RequestMapping("/api/grados-secciones")
@RequierePerfil(Perfil.ADMINISTRADOR)
public class GradoSeccionRest {

    private final GradoSeccionService service;

    public GradoSeccionRest(GradoSeccionService service) {
        this.service = service;
    }

    @Operation(summary = "Listar con la cantidad de estudiantes activos (estado: A activos, I inactivos, T todos; filtros: nivel, grado)")
    @RequierePerfil({Perfil.ADMINISTRADOR, Perfil.DOCENTE})
    @GetMapping
    public List<GradoSeccionResponse> listar(@RequestParam(required = false) String estado, @RequestParam(required = false) Nivel nivel,
                                             @RequestParam(required = false) String grado) {
        return service.listar(nivel, grado, estado);
    }

    @RequierePerfil({Perfil.ADMINISTRADOR, Perfil.DOCENTE})
    @GetMapping("/{id}")
    public GradoSeccionResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @Operation(summary = "Crear (sin turno: la sección C es de tarde, las demás de mañana)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GradoSeccionResponse crear(@Valid @RequestBody GradoSeccionRequest request) {
        return service.crear(request);
    }

    @PutMapping("/{id}")
    public GradoSeccionResponse actualizar(@PathVariable Long id, @Valid @RequestBody GradoSeccionRequest request) {
        return service.actualizar(id, request);
    }

    @Operation(summary = "Eliminar lógico (estado I; solo sin estudiantes ni cursos activos)")
    @PatchMapping("/{id}/eliminar")
    public GradoSeccionResponse eliminar(@PathVariable Long id) {
        return service.eliminar(id);
    }

    @Operation(summary = "Restaurar lógico (estado A)")
    @PatchMapping("/{id}/restaurar")
    public GradoSeccionResponse restaurar(@PathVariable Long id) {
        return service.restaurar(id);
    }
}
