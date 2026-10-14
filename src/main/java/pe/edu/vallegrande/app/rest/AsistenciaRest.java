package pe.edu.vallegrande.app.rest;

import java.time.LocalDate;
import java.util.List;

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
import lombok.RequiredArgsConstructor;
import pe.edu.vallegrande.app.dto.request.AsistenciaLoteRequest;
import pe.edu.vallegrande.app.dto.request.EstadoAsistenciaRequest;
import pe.edu.vallegrande.app.dto.response.AsistenciaResponses.AsistenciaDiaResponse;
import pe.edu.vallegrande.app.dto.response.AsistenciaResponses.MatrizMensualResponse;
import pe.edu.vallegrande.app.dto.response.AsistenciaResponses.RegistroDia;
import pe.edu.vallegrande.app.dto.response.AsistenciaResponses.RegistroHistorial;
import pe.edu.vallegrande.app.model.enums.Perfil;
import pe.edu.vallegrande.app.seguridad.RequierePerfil;
import pe.edu.vallegrande.app.seguridad.SesionUsuario;
import pe.edu.vallegrande.app.service.AsistenciaService;

/** Matriz de asistencia diaria por aula (P/T/F/J). */
@Tag(name = "Asistencia diaria por aula")
@RestController
@RequestMapping("/api/asistencia")
@RequiredArgsConstructor
@RequierePerfil({Perfil.ADMINISTRADOR, Perfil.DOCENTE})
public class AsistenciaRest {

    private final AsistenciaService service;

    @Operation(summary = "Registros diarios de todas las matrículas activas (o de una sección) entre dos fechas (máx. 93 días)")
    @GetMapping
    public List<RegistroDia> rango(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                   @RequestParam(required = false) Long seccionId) {
        return service.rango(desde, hasta, seccionId);
    }

    @Operation(summary = "Registro del aula en una fecha (por defecto hoy); los no registrados figuran como pendiente")
    @GetMapping("/secciones/{seccionId}")
    public AsistenciaDiaResponse delAula(@PathVariable Long seccionId,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return service.delAula(seccionId, fecha);
    }

    @Operation(summary = "Guardar la asistencia del aula en una fecha (no futura ni fin de semana)")
    @PutMapping("/secciones/{seccionId}")
    public AsistenciaDiaResponse registrarLote(@PathVariable Long seccionId, @Valid @RequestBody AsistenciaLoteRequest request,
                                               SesionUsuario sesion) {
        return service.registrarLote(seccionId, request, sesion);
    }

    @Operation(summary = "Vista mensual del aula (mes AAAA-MM, por defecto el actual)")
    @GetMapping("/secciones/{seccionId}/mensual")
    public MatrizMensualResponse mensual(@PathVariable Long seccionId, @RequestParam(required = false) String mes) {
        return service.mensual(seccionId, mes);
    }

    @Operation(summary = "Cambiar el estado de un estudiante en una fecha (\"pendiente\" borra el registro)")
    @PutMapping("/estudiantes/{estudianteId}/{fecha}")
    public RegistroHistorial registrar(@PathVariable Long estudianteId,
                                       @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                                       @Valid @RequestBody EstadoAsistenciaRequest request, SesionUsuario sesion) {
        return service.registrar(estudianteId, fecha, request.estado(), sesion);
    }
}
