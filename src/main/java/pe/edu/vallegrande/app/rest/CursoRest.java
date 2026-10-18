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
import pe.edu.vallegrande.app.dto.request.CursoRequest;
import pe.edu.vallegrande.app.dto.response.CursoResponse;
import pe.edu.vallegrande.app.model.enums.Perfil;
import pe.edu.vallegrande.app.seguridad.RequierePerfil;
import pe.edu.vallegrande.app.seguridad.SesionUsuario;
import pe.edu.vallegrande.app.service.CursoService;
import pe.edu.vallegrande.app.service.EstudianteService;
import pe.edu.vallegrande.app.util.FiltroEstado;

/** Cursos por grado y sección, cada uno con su docente. */
@Tag(name = "Cursos")
@RestController
@RequestMapping("/api/cursos")
@RequierePerfil(Perfil.ADMINISTRADOR)
public class CursoRest {

    private final CursoService service;
    private final EstudianteService estudianteService;

    public CursoRest(CursoService service, EstudianteService estudianteService) {
        this.service = service;
        this.estudianteService = estudianteService;
    }

    @Operation(summary = "Listar (estado: A activos, I inactivos, T todos; filtros: gradoSeccionId, docenteId). Un alumno solo ve los de su aula.")
    @RequierePerfil({Perfil.ADMINISTRADOR, Perfil.DOCENTE, Perfil.ALUMNO})
    @GetMapping
    public List<CursoResponse> listar(@RequestParam(required = false) String estado, @RequestParam(required = false) Long gradoSeccionId,
                                      @RequestParam(required = false) Long docenteId, SesionUsuario sesion) {
        if (sesion.esAlumno()) {
            return service.listar(estudianteService.obtener(sesion.estudianteId()).gradoSeccionId(), null, FiltroEstado.ACTIVO);
        }
        return service.listar(gradoSeccionId, docenteId, estado);
    }

    @RequierePerfil({Perfil.ADMINISTRADOR, Perfil.DOCENTE})
    @GetMapping("/{id}")
    public CursoResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @Operation(summary = "Crear curso en un aula con su docente (cargo Docente)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CursoResponse crear(@Valid @RequestBody CursoRequest request) {
        return service.crear(request);
    }

    @PutMapping("/{id}")
    public CursoResponse actualizar(@PathVariable Long id, @Valid @RequestBody CursoRequest request) {
        return service.actualizar(id, request);
    }

    @Operation(summary = "Eliminar lógico (estado I): conserva su asistencia y justificaciones")
    @PatchMapping("/{id}/eliminar")
    public CursoResponse eliminar(@PathVariable Long id) {
        return service.eliminar(id);
    }

    @Operation(summary = "Restaurar lógico (estado A)")
    @PatchMapping("/{id}/restaurar")
    public CursoResponse restaurar(@PathVariable Long id) {
        return service.restaurar(id);
    }
}
