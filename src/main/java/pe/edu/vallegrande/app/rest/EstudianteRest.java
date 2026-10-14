package pe.edu.vallegrande.app.rest;

import java.time.LocalDate;
import java.util.List;

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
import lombok.RequiredArgsConstructor;
import pe.edu.vallegrande.app.dto.request.ContactoRequest;
import pe.edu.vallegrande.app.dto.request.EstudianteRequest;
import pe.edu.vallegrande.app.dto.response.AlertaResponses.AvisoResponse;
import pe.edu.vallegrande.app.dto.response.AsistenciaResponses.RegistroHistorial;
import pe.edu.vallegrande.app.dto.response.EstudianteResponse;
import pe.edu.vallegrande.app.model.enums.Perfil;
import pe.edu.vallegrande.app.seguridad.RequierePerfil;
import pe.edu.vallegrande.app.seguridad.SesionUsuario;
import pe.edu.vallegrande.app.service.AlertaService;
import pe.edu.vallegrande.app.service.AsistenciaService;
import pe.edu.vallegrande.app.service.EstudianteService;

@Tag(name = "Estudiantes")
@RestController
@RequestMapping("/api/estudiantes")
@RequiredArgsConstructor
@RequierePerfil(Perfil.ADMINISTRADOR)
public class EstudianteRest {

    private final EstudianteService service;
    private final AsistenciaService asistenciaService;
    private final AlertaService alertaService;

    @RequierePerfil({Perfil.ADMINISTRADOR, Perfil.DOCENTE, Perfil.ALUMNO})
    @GetMapping("/{id}")
    public EstudianteResponse obtener(@PathVariable Long id, SesionUsuario sesion) {
        sesion.verificarEstudiante(id);
        return service.obtener(id);
    }

    @Operation(summary = "Corregir datos personales")
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

    @Operation(summary = "Eliminar (solo si no tiene historial)")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        service.eliminar(id);
    }

    @Operation(summary = "Asistencia diaria registrada del estudiante")
    @RequierePerfil({Perfil.ADMINISTRADOR, Perfil.DOCENTE, Perfil.ALUMNO})
    @GetMapping("/{id}/asistencia")
    public List<RegistroHistorial> asistencia(@PathVariable Long id,
                                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                              SesionUsuario sesion) {
        sesion.verificarEstudiante(id);
        return asistenciaService.historial(id, desde, hasta);
    }

    @Operation(summary = "Avisos enviados a la familia")
    @RequierePerfil({Perfil.ADMINISTRADOR, Perfil.DOCENTE})
    @GetMapping("/{id}/avisos")
    public List<AvisoResponse> avisos(@PathVariable Long id) {
        return alertaService.avisos(id);
    }
}
