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
import pe.edu.vallegrande.app.dto.request.DocenteRequest;
import pe.edu.vallegrande.app.dto.response.CursoResponse;
import pe.edu.vallegrande.app.dto.response.DocenteResponse;
import pe.edu.vallegrande.app.model.enums.Perfil;
import pe.edu.vallegrande.app.seguridad.RequierePerfil;
import pe.edu.vallegrande.app.seguridad.SesionUsuario;
import pe.edu.vallegrande.app.service.CursoService;
import pe.edu.vallegrande.app.service.DocenteService;
import pe.edu.vallegrande.app.util.FiltroEstado;

/** Docentes y demás personal (auxiliares, administrativos, directivos, servicio): eliminación y restauración lógicas. */
@Tag(name = "Docentes y personal")
@RestController
@RequestMapping("/api/docentes")
@RequierePerfil(Perfil.ADMINISTRADOR)
public class DocenteRest {

    private final DocenteService service;
    private final CursoService cursoService;

    public DocenteRest(DocenteService service, CursoService cursoService) {
        this.service = service;
        this.cursoService = cursoService;
    }

    @Operation(summary = "Listar (estado: A activos, I inactivos, T todos; cargo: Docente, Auxiliar, Administrativo, Directivo, Servicio)")
    @GetMapping
    public List<DocenteResponse> listar(@RequestParam(required = false) String estado, @RequestParam(required = false) String cargo) {
        return service.listar(estado, cargo);
    }

    @Operation(summary = "Obtener por ID (un docente solo su propia ficha)")
    @RequierePerfil({Perfil.ADMINISTRADOR, Perfil.DOCENTE})
    @GetMapping("/{id}")
    public DocenteResponse obtener(@PathVariable Long id, SesionUsuario sesion) {
        sesion.verificarDocente(id);
        return service.obtener(id);
    }

    @Operation(summary = "Crear")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DocenteResponse crear(@Valid @RequestBody DocenteRequest request) {
        return service.crear(request);
    }

    @Operation(summary = "Editar")
    @PutMapping("/{id}")
    public DocenteResponse editar(@PathVariable Long id, @Valid @RequestBody DocenteRequest request) {
        return service.editar(id, request);
    }

    @Operation(summary = "Eliminar lógico (estado I)")
    @PatchMapping("/{id}/eliminar")
    public DocenteResponse eliminar(@PathVariable Long id) {
        return service.eliminar(id);
    }

    @Operation(summary = "Restaurar lógico (estado A)")
    @PatchMapping("/{id}/restaurar")
    public DocenteResponse restaurar(@PathVariable Long id) {
        return service.restaurar(id);
    }

    @Operation(summary = "Cursos que dicta")
    @RequierePerfil({Perfil.ADMINISTRADOR, Perfil.DOCENTE})
    @GetMapping("/{id}/cursos")
    public List<CursoResponse> cursos(@PathVariable Long id, SesionUsuario sesion) {
        sesion.verificarDocente(id);
        service.obtener(id);
        return cursoService.listar(null, id, FiltroEstado.ACTIVO);
    }
}
