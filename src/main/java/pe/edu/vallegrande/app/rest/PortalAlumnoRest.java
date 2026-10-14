package pe.edu.vallegrande.app.rest;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import pe.edu.vallegrande.app.dto.response.AsignacionResponse;
import pe.edu.vallegrande.app.dto.response.EstudianteResponse;
import pe.edu.vallegrande.app.dto.response.PortalAlumnoResponses.AvisoAlumnoResponse;
import pe.edu.vallegrande.app.dto.response.PortalAlumnoResponses.DiaAlumnoResponse;
import pe.edu.vallegrande.app.dto.response.PortalAlumnoResponses.InicioAlumnoResponse;
import pe.edu.vallegrande.app.dto.response.PortalAlumnoResponses.ResumenAsistenciaResponse;
import pe.edu.vallegrande.app.model.enums.Perfil;
import pe.edu.vallegrande.app.seguridad.RequierePerfil;
import pe.edu.vallegrande.app.seguridad.SesionUsuario;
import pe.edu.vallegrande.app.service.PortalAlumnoService;

/** Portal del alumno. El alumno solo accede a su propio id; administrador y docente pueden consultar cualquiera. */
@Tag(name = "Portal del alumno")
@RestController
@RequestMapping("/api/alumnos/{estudianteId}")
@RequiredArgsConstructor
@RequierePerfil({Perfil.ADMINISTRADOR, Perfil.DOCENTE, Perfil.ALUMNO})
public class PortalAlumnoRest {

    private final PortalAlumnoService service;

    @GetMapping("/perfil")
    public EstudianteResponse perfil(@PathVariable Long estudianteId, SesionUsuario sesion) {
        sesion.verificarEstudiante(estudianteId);
        return service.perfil(estudianteId);
    }

    @Operation(summary = "Pantalla de inicio: estado de hoy, resumen y pendientes")
    @GetMapping("/inicio")
    public InicioAlumnoResponse inicio(@PathVariable Long estudianteId, SesionUsuario sesion) {
        sesion.verificarEstudiante(estudianteId);
        return service.inicio(estudianteId);
    }

    @Operation(summary = "Mis cursos y Mi horario (docente y bloques por día)")
    @GetMapping("/cursos")
    public List<AsignacionResponse> cursos(@PathVariable Long estudianteId, SesionUsuario sesion) {
        sesion.verificarEstudiante(estudianteId);
        return service.cursos(estudianteId);
    }

    @Operation(summary = "Mi asistencia: historial por curso y día (sin desde: últimos 20 registros)")
    @GetMapping("/asistencia")
    public List<DiaAlumnoResponse> asistencia(@PathVariable Long estudianteId,
                                              @RequestParam(required = false) Long cursoId,
                                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                              @RequestParam(required = false) Integer limite,
                                              SesionUsuario sesion) {
        sesion.verificarEstudiante(estudianteId);
        return service.asistencia(estudianteId, cursoId, desde, hasta, limite);
    }

    @GetMapping("/asistencia/resumen")
    public ResumenAsistenciaResponse resumen(@PathVariable Long estudianteId,
                                             @RequestParam(required = false) Long cursoId,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                             SesionUsuario sesion) {
        sesion.verificarEstudiante(estudianteId);
        return service.resumen(estudianteId, cursoId, desde, hasta);
    }

    @GetMapping("/alertas")
    public List<AvisoAlumnoResponse> alertas(@PathVariable Long estudianteId, SesionUsuario sesion) {
        sesion.verificarEstudiante(estudianteId);
        return service.alertas(estudianteId);
    }

    @GetMapping("/comunicaciones")
    public List<AvisoAlumnoResponse> comunicaciones(@PathVariable Long estudianteId, SesionUsuario sesion) {
        sesion.verificarEstudiante(estudianteId);
        return service.comunicaciones(estudianteId);
    }
}
