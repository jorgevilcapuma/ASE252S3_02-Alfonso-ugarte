package pe.edu.vallegrande.app.rest;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import pe.edu.vallegrande.app.dto.request.AsistenciaLoteRequest;
import pe.edu.vallegrande.app.dto.request.EstadoAsistenciaRequest;
import pe.edu.vallegrande.app.dto.response.AsistenciaResponses.AsistenciaDiaResponse;
import pe.edu.vallegrande.app.dto.response.AsistenciaResponses.MatrizMensualResponse;
import pe.edu.vallegrande.app.dto.response.AsistenciaResponses.RegistroHistorial;
import pe.edu.vallegrande.app.model.enums.Perfil;
import pe.edu.vallegrande.app.seguridad.RequierePerfil;
import pe.edu.vallegrande.app.seguridad.SesionUsuario;
import pe.edu.vallegrande.app.service.AsistenciaEstudianteService;

/** Asistencia de estudiantes (P/T/F/J): sin cursoId es la asistencia diaria del aula; con cursoId, la de esa clase. */
@Tag(name = "Asistencia de estudiantes")
@RestController
@RequestMapping("/api/asistencia-estudiantes")
@RequierePerfil({Perfil.ADMINISTRADOR, Perfil.DOCENTE})
public class AsistenciaEstudianteRest {

    private final AsistenciaEstudianteService service;

    public AsistenciaEstudianteRest(AsistenciaEstudianteService service) {
        this.service = service;
    }

    @Operation(summary = "Asistencia del aula en una fecha (por defecto hoy); los no registrados figuran como pendiente")
    @GetMapping("/grados-secciones/{gradoSeccionId}")
    public AsistenciaDiaResponse delAula(@PathVariable Long gradoSeccionId,
                                         @RequestParam(required = false) Long cursoId,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return service.delAula(gradoSeccionId, cursoId, fecha);
    }

    @Operation(summary = "Guardar la asistencia del aula en una fecha (no futura ni fin de semana)")
    @PutMapping("/grados-secciones/{gradoSeccionId}")
    public AsistenciaDiaResponse registrarLote(@PathVariable Long gradoSeccionId, @Valid @RequestBody AsistenciaLoteRequest request,
                                               SesionUsuario sesion) {
        return service.registrarLote(gradoSeccionId, request, sesion);
    }

    @Operation(summary = "Vista mensual del aula (mes AAAA-MM, por defecto el actual)")
    @GetMapping("/grados-secciones/{gradoSeccionId}/mensual")
    public MatrizMensualResponse mensual(@PathVariable Long gradoSeccionId,
                                         @RequestParam(required = false) Long cursoId,
                                         @RequestParam(required = false) String mes) {
        return service.mensual(gradoSeccionId, cursoId, mes);
    }

    @Operation(summary = "Cambiar la asistencia de un estudiante en una fecha (\"pendiente\" la borra)")
    @PutMapping("/estudiantes/{estudianteId}")
    public RegistroHistorial registrar(@PathVariable Long estudianteId, @Valid @RequestBody EstadoAsistenciaRequest request,
                                       SesionUsuario sesion) {
        return service.registrar(estudianteId, request, sesion);
    }
}
