package pe.edu.vallegrande.app.rest;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import pe.edu.vallegrande.app.dto.request.MarcacionPersonalRequest;
import pe.edu.vallegrande.app.dto.response.AsistenciaPersonalResponses.AsistenciaPersonalDiaResponse;
import pe.edu.vallegrande.app.dto.response.AsistenciaPersonalResponses.FilaPersonal;
import pe.edu.vallegrande.app.dto.response.AsistenciaPersonalResponses.ReportePersonalResponse;
import pe.edu.vallegrande.app.model.enums.Perfil;
import pe.edu.vallegrande.app.seguridad.RequierePerfil;
import pe.edu.vallegrande.app.seguridad.SesionUsuario;
import pe.edu.vallegrande.app.service.AsistenciaPersonalService;

/** Control de entrada y salida del personal, separado de la asistencia de los estudiantes. */
@Tag(name = "Asistencia del personal")
@RestController
@RequestMapping("/api/asistencia-personal")
@RequierePerfil(Perfil.ADMINISTRADOR)
public class AsistenciaPersonalRest {

    private final AsistenciaPersonalService service;

    public AsistenciaPersonalRest(AsistenciaPersonalService service) {
        this.service = service;
    }

    @Operation(summary = "Marcaciones del día (por defecto hoy): todos los docentes y personal activos, marcados o no")
    @GetMapping
    public AsistenciaPersonalDiaResponse delDia(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return service.delDia(fecha);
    }

    @Operation(summary = "Reporte mensual por persona (mes AAAA-MM, por defecto el actual)")
    @GetMapping("/reporte")
    public ReportePersonalResponse reporte(@RequestParam(required = false) String mes) {
        return service.reporte(mes);
    }

    @Operation(summary = "Marcar (o corregir) la entrada: puntual o tardanza según la hora de entrada y la tolerancia")
    @PutMapping("/{docenteId}/entrada")
    public FilaPersonal entrada(@PathVariable Long docenteId, @Valid @RequestBody MarcacionPersonalRequest request, SesionUsuario sesion) {
        return service.marcarEntrada(docenteId, request, sesion);
    }

    @Operation(summary = "Marcar (o corregir) la salida; requiere la entrada del mismo día")
    @PutMapping("/{docenteId}/salida")
    public FilaPersonal salida(@PathVariable Long docenteId, @Valid @RequestBody MarcacionPersonalRequest request, SesionUsuario sesion) {
        return service.marcarSalida(docenteId, request, sesion);
    }

    @Operation(summary = "Anular la marcación de una fecha")
    @DeleteMapping("/{docenteId}/{fecha}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long docenteId, @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        service.eliminar(docenteId, fecha);
    }
}
