package pe.edu.vallegrande.app.rest;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import pe.edu.vallegrande.app.dto.request.DocenteRequest;
import pe.edu.vallegrande.app.dto.request.PerfilDocenteRequest;
import pe.edu.vallegrande.app.dto.response.AlumnoCursoResponse;
import pe.edu.vallegrande.app.dto.response.AsignacionResponse;
import pe.edu.vallegrande.app.dto.response.DocenteResponse;
import pe.edu.vallegrande.app.dto.response.SesionClaseResponses.AlertaCursoResponse;
import pe.edu.vallegrande.app.dto.response.SesionClaseResponses.SesionHistorialResponse;
import pe.edu.vallegrande.app.model.enums.Perfil;
import pe.edu.vallegrande.app.seguridad.RequierePerfil;
import pe.edu.vallegrande.app.seguridad.SesionUsuario;
import pe.edu.vallegrande.app.service.AsignacionService;
import pe.edu.vallegrande.app.service.DocenteService;
import pe.edu.vallegrande.app.service.SesionClaseService;

/** Fichas de docentes (administrador) y el portal del docente: perfil, cursos, estudiantes, historial y alertas. */
@Tag(name = "Docentes")
@RestController
@RequestMapping("/api/docentes")
@RequiredArgsConstructor
@RequierePerfil(Perfil.ADMINISTRADOR)
public class DocenteRest {

    private final DocenteService service;
    private final AsignacionService asignacionService;
    private final SesionClaseService sesionService;

    @GetMapping
    public List<DocenteResponse> listar() {
        return service.listar();
    }

    @RequierePerfil({Perfil.ADMINISTRADOR, Perfil.DOCENTE})
    @GetMapping("/{id}")
    public DocenteResponse obtener(@PathVariable Long id, SesionUsuario sesion) {
        sesion.verificarDocente(id);
        return service.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DocenteResponse crear(@Valid @RequestBody DocenteRequest request) {
        return service.crear(request);
    }

    @PutMapping("/{id}")
    public DocenteResponse actualizar(@PathVariable Long id, @Valid @RequestBody DocenteRequest request) {
        return service.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        service.eliminar(id);
    }

    @Operation(summary = "Mi perfil: nombres, apellidos y teléfono")
    @RequierePerfil({Perfil.ADMINISTRADOR, Perfil.DOCENTE})
    @PutMapping("/{id}/perfil")
    public DocenteResponse actualizarPerfil(@PathVariable Long id, @Valid @RequestBody PerfilDocenteRequest request, SesionUsuario sesion) {
        sesion.verificarDocente(id);
        return service.actualizarPerfil(id, request);
    }

    @Operation(summary = "Mis cursos")
    @RequierePerfil({Perfil.ADMINISTRADOR, Perfil.DOCENTE})
    @GetMapping("/{id}/cursos")
    public List<AsignacionResponse> cursos(@PathVariable Long id, SesionUsuario sesion) {
        sesion.verificarDocente(id);
        service.obtener(id);
        return asignacionService.listar(id, null);
    }

    @Operation(summary = "Mis estudiantes con porcentaje de asistencia por curso")
    @RequierePerfil({Perfil.ADMINISTRADOR, Perfil.DOCENTE})
    @GetMapping("/{id}/estudiantes")
    public List<AlumnoCursoResponse> estudiantes(@PathVariable Long id, @RequestParam(required = false) Long cursoId, SesionUsuario sesion) {
        sesion.verificarDocente(id);
        return service.estudiantes(id, cursoId);
    }

    @Operation(summary = "Historial de sesiones de asistencia guardadas")
    @RequierePerfil({Perfil.ADMINISTRADOR, Perfil.DOCENTE})
    @GetMapping("/{id}/historial")
    public List<SesionHistorialResponse> historial(@PathVariable Long id,
                                                   @RequestParam(required = false) Long cursoId,
                                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                                   SesionUsuario sesion) {
        sesion.verificarDocente(id);
        service.obtener(id);
        return sesionService.historial(id, cursoId, desde, hasta);
    }

    @Operation(summary = "Alumnos en riesgo en sus cursos")
    @RequierePerfil({Perfil.ADMINISTRADOR, Perfil.DOCENTE})
    @GetMapping("/{id}/alertas")
    public List<AlertaCursoResponse> alertas(@PathVariable Long id, SesionUsuario sesion) {
        sesion.verificarDocente(id);
        return service.alertas(id);
    }
}
