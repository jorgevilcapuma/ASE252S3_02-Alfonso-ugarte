package pe.edu.vallegrande.app.rest;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
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
import pe.edu.vallegrande.app.dto.request.ContactoRequest;
import pe.edu.vallegrande.app.dto.request.EstudianteRequest;
import pe.edu.vallegrande.app.dto.response.AsistenciaResponses.RegistroHistorial;
import pe.edu.vallegrande.app.dto.response.EstudianteResponse;
import pe.edu.vallegrande.app.model.enums.Perfil;
import pe.edu.vallegrande.app.seguridad.RequierePerfil;
import pe.edu.vallegrande.app.seguridad.SesionUsuario;
import pe.edu.vallegrande.app.service.AsistenciaEstudianteService;
import pe.edu.vallegrande.app.service.EstudianteService;

@Tag(name = "Estudiantes")
@RestController
@RequestMapping("/api/estudiantes")
@RequierePerfil(Perfil.ADMINISTRADOR)
public class EstudianteRest {

    private final EstudianteService service;
    private final AsistenciaEstudianteService asistenciaService;

    public EstudianteRest(EstudianteService service, AsistenciaEstudianteService asistenciaService) {
        this.service = service;
        this.asistenciaService = asistenciaService;
    }

    @Operation(summary = "Listar (filtros: gradoSeccionId, estado A/I/T —por defecto A—, q en nombres, apellidos o DNI)")
    @RequierePerfil({Perfil.ADMINISTRADOR, Perfil.DOCENTE})
    @GetMapping
    public List<EstudianteResponse> listar(@RequestParam(required = false) Long gradoSeccionId,
                                           @RequestParam(required = false) String estado,
                                           @RequestParam(required = false) String q) {
        return service.listar(gradoSeccionId, estado, q);
    }

    @Operation(summary = "Obtener por ID (un alumno solo el suyo)")
    @RequierePerfil({Perfil.ADMINISTRADOR, Perfil.DOCENTE, Perfil.ALUMNO})
    @GetMapping("/{id}")
    public EstudianteResponse obtener(@PathVariable Long id, SesionUsuario sesion) {
        sesion.verificarEstudiante(id);
        return service.obtener(id);
    }

    @Operation(summary = "Registrar estudiante en un grado y sección")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EstudianteResponse crear(@Valid @RequestBody EstudianteRequest request) {
        return service.crear(request);
    }

    @Operation(summary = "Editar datos o cambiar de grado y sección")
    @PutMapping("/{id}")
    public EstudianteResponse actualizar(@PathVariable Long id, @Valid @RequestBody EstudianteRequest request) {
        return service.actualizar(id, request);
    }

    @Operation(summary = "Contacto (teléfono y correo) desde Mi perfil del alumno")
    @RequierePerfil({Perfil.ADMINISTRADOR, Perfil.ALUMNO})
    @PutMapping("/{id}/contacto")
    public EstudianteResponse actualizarContacto(@PathVariable Long id, @Valid @RequestBody ContactoRequest request, SesionUsuario sesion) {
        sesion.verificarEstudiante(id);
        return service.actualizarContacto(id, request);
    }

    @Operation(summary = "Retiro lógico (estado I): conserva su historial")
    @PatchMapping("/{id}/eliminar")
    public EstudianteResponse eliminar(@PathVariable Long id) {
        return service.eliminar(id);
    }

    @Operation(summary = "Restaurar lógico (estado A)")
    @PatchMapping("/{id}/restaurar")
    public EstudianteResponse restaurar(@PathVariable Long id) {
        return service.restaurar(id);
    }

    @Operation(summary = "Historial de asistencia (diaria y por curso), por defecto los últimos 60 días")
    @RequierePerfil({Perfil.ADMINISTRADOR, Perfil.DOCENTE, Perfil.ALUMNO})
    @GetMapping("/{id}/asistencia")
    public List<RegistroHistorial> asistencia(@PathVariable Long id,
                                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                              SesionUsuario sesion) {
        sesion.verificarEstudiante(id);
        return asistenciaService.historial(id, desde, hasta);
    }
}
