package pe.edu.vallegrande.app.rest;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
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
import pe.edu.vallegrande.app.dto.request.JustificacionRequest;
import pe.edu.vallegrande.app.dto.request.RevisionJustificacionRequest;
import pe.edu.vallegrande.app.dto.response.JustificacionResponse;
import pe.edu.vallegrande.app.model.enums.EstadoJustificacion;
import pe.edu.vallegrande.app.model.enums.Perfil;
import pe.edu.vallegrande.app.seguridad.RequierePerfil;
import pe.edu.vallegrande.app.seguridad.SesionUsuario;
import pe.edu.vallegrande.app.service.JustificacionService;

@Tag(name = "Justificaciones")
@RestController
@RequestMapping("/api/justificaciones")
@RequiredArgsConstructor
@RequierePerfil({Perfil.ADMINISTRADOR, Perfil.DOCENTE, Perfil.ALUMNO})
public class JustificacionRest {

    private final JustificacionService service;

    @Operation(summary = "Listar (un alumno solo ve las suyas)")
    @GetMapping
    public List<JustificacionResponse> listar(@RequestParam(required = false) EstadoJustificacion estado,
                                              @RequestParam(required = false) Long estudianteId,
                                              @RequestParam(required = false) Long cursoId,
                                              @RequestParam(required = false) Long docenteId,
                                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                              SesionUsuario sesion) {
        return service.listar(estado, estudianteId, cursoId, docenteId, desde, hasta, sesion);
    }

    @GetMapping("/{id}")
    public JustificacionResponse obtener(@PathVariable Long id, SesionUsuario sesion) {
        return service.obtener(id, sesion);
    }

    @Operation(summary = "Solicitar justificación (el alumno no envía estudianteId: se toma de su sesión)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public JustificacionResponse crear(@Valid @RequestBody JustificacionRequest request, SesionUsuario sesion) {
        return service.crear(request, sesion);
    }

    @Operation(summary = "Editar una solicitud pendiente")
    @PutMapping("/{id}")
    public JustificacionResponse actualizar(@PathVariable Long id, @Valid @RequestBody JustificacionRequest request, SesionUsuario sesion) {
        return service.actualizar(id, request, sesion);
    }

    @Operation(summary = "Aprobar u observar (actualiza la asistencia del día y del curso)")
    @RequierePerfil({Perfil.ADMINISTRADOR, Perfil.DOCENTE})
    @PatchMapping("/{id}/revision")
    public JustificacionResponse revisar(@PathVariable Long id, @Valid @RequestBody RevisionJustificacionRequest request, SesionUsuario sesion) {
        return service.revisar(id, request, sesion);
    }

    @Operation(summary = "Retirar una solicitud pendiente")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id, SesionUsuario sesion) {
        service.eliminar(id, sesion);
    }
}
