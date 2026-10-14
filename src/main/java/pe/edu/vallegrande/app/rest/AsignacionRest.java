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
import pe.edu.vallegrande.app.dto.request.AsignacionRequest;
import pe.edu.vallegrande.app.dto.request.SesionClaseRequest;
import pe.edu.vallegrande.app.dto.response.AsignacionResponse;
import pe.edu.vallegrande.app.dto.response.SesionClaseResponses.ResumenMensualResponse;
import pe.edu.vallegrande.app.dto.response.SesionClaseResponses.SesionClaseResponse;
import pe.edu.vallegrande.app.dto.response.SesionClaseResponses.SesionHistorialResponse;
import pe.edu.vallegrande.app.model.enums.Perfil;
import pe.edu.vallegrande.app.seguridad.RequierePerfil;
import pe.edu.vallegrande.app.seguridad.SesionUsuario;
import pe.edu.vallegrande.app.service.AsignacionService;
import pe.edu.vallegrande.app.service.SesionClaseService;

/**
 * Cursos por sección (curso + sección + docente + horario) y su asistencia por sesión.
 * En el frontend del docente cada asignación es un "Curso".
 */
@Tag(name = "Cursos por sección y asistencia por curso")
@RestController
@RequestMapping("/api/asignaciones")
@RequiredArgsConstructor
@RequierePerfil(Perfil.ADMINISTRADOR)
public class AsignacionRest {

    private final AsignacionService service;
    private final SesionClaseService sesionService;

    @Operation(summary = "Listar cursos por sección; un docente solo ve los suyos")
    @RequierePerfil({Perfil.ADMINISTRADOR, Perfil.DOCENTE})
    @GetMapping
    public List<AsignacionResponse> listar(@RequestParam(required = false) Long docenteId, @RequestParam(required = false) Long seccionId,
                                           SesionUsuario sesion) {
        return service.listar(sesion.esDocente() ? sesion.docenteFiltro() : docenteId, seccionId);
    }

    @RequierePerfil({Perfil.ADMINISTRADOR, Perfil.DOCENTE})
    @GetMapping("/{id}")
    public AsignacionResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AsignacionResponse crear(@Valid @RequestBody AsignacionRequest request) {
        return service.crear(request);
    }

    @PutMapping("/{id}")
    public AsignacionResponse actualizar(@PathVariable Long id, @Valid @RequestBody AsignacionRequest request) {
        return service.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        service.eliminar(id);
    }

    // ---------- Asistencia por curso ----------

    @Operation(summary = "Lista para tomar asistencia del curso en una fecha (yaRegistrado indica si existe)")
    @RequierePerfil({Perfil.ADMINISTRADOR, Perfil.DOCENTE})
    @GetMapping("/{id}/sesiones/{fecha}")
    public SesionClaseResponse sesion(@PathVariable Long id, @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                                      SesionUsuario sesion) {
        return sesionService.obtener(id, fecha, sesion);
    }

    @Operation(summary = "Guardar o actualizar la asistencia del curso en una fecha")
    @RequierePerfil({Perfil.ADMINISTRADOR, Perfil.DOCENTE})
    @PutMapping("/{id}/sesiones/{fecha}")
    public SesionClaseResponse guardarSesion(@PathVariable Long id, @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                                             @Valid @RequestBody SesionClaseRequest request, SesionUsuario sesion) {
        return sesionService.guardar(id, fecha, request, sesion);
    }

    @RequierePerfil({Perfil.ADMINISTRADOR, Perfil.DOCENTE})
    @DeleteMapping("/{id}/sesiones/{fecha}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarSesion(@PathVariable Long id, @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                               SesionUsuario sesion) {
        sesionService.eliminar(id, fecha, sesion);
    }

    @Operation(summary = "Sesiones registradas del curso (historial)")
    @RequierePerfil({Perfil.ADMINISTRADOR, Perfil.DOCENTE})
    @GetMapping("/{id}/sesiones")
    public List<SesionHistorialResponse> historial(@PathVariable Long id,
                                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                                   SesionUsuario sesion) {
        return sesionService.historial(sesion.esDocente() ? sesion.docenteFiltro() : null, id, desde, hasta);
    }

    @Operation(summary = "Reporte mensual por alumno (mes AAAA-MM)")
    @RequierePerfil({Perfil.ADMINISTRADOR, Perfil.DOCENTE})
    @GetMapping("/{id}/resumen-mensual")
    public List<ResumenMensualResponse> resumenMensual(@PathVariable Long id, @RequestParam(required = false) String mes, SesionUsuario sesion) {
        return sesionService.resumenMensual(id, mes, sesion);
    }
}
