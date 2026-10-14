package pe.edu.vallegrande.app.rest;

import java.util.List;

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
import pe.edu.vallegrande.app.dto.request.GradoRequest;
import pe.edu.vallegrande.app.dto.request.NivelRequest;
import pe.edu.vallegrande.app.dto.request.SeccionRequest;
import pe.edu.vallegrande.app.dto.response.EstructuraResponse.GradoResponse;
import pe.edu.vallegrande.app.dto.response.EstructuraResponse.NivelResponse;
import pe.edu.vallegrande.app.dto.response.EstructuraResponse.SeccionResponse;
import pe.edu.vallegrande.app.dto.response.EstudianteResponse;
import pe.edu.vallegrande.app.model.enums.Perfil;
import pe.edu.vallegrande.app.seguridad.RequierePerfil;
import pe.edu.vallegrande.app.service.EstructuraService;
import pe.edu.vallegrande.app.service.MatriculaService;

/** Tablas maestras: niveles, grados y secciones. Lectura para cualquier sesión; cambios solo del administrador. */
@Tag(name = "Estructura (niveles, grados, secciones)")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@RequierePerfil(Perfil.ADMINISTRADOR)
public class EstructuraRest {

    private final EstructuraService service;
    private final MatriculaService matriculaService;

    @Operation(summary = "Árbol nivel → grados → secciones (pantalla Selección de sección)")
    @RequierePerfil({Perfil.ADMINISTRADOR, Perfil.DOCENTE, Perfil.ALUMNO})
    @GetMapping("/estructura")
    public List<NivelResponse> estructura() {
        return service.estructura();
    }

    // ---------- Niveles ----------

    @RequierePerfil({Perfil.ADMINISTRADOR, Perfil.DOCENTE, Perfil.ALUMNO})
    @GetMapping("/niveles")
    public List<NivelResponse> listarNiveles() {
        return service.listarNiveles();
    }

    @PostMapping("/niveles")
    @ResponseStatus(HttpStatus.CREATED)
    public NivelResponse crearNivel(@Valid @RequestBody NivelRequest request) {
        return service.crearNivel(request);
    }

    @PutMapping("/niveles/{id}")
    public NivelResponse actualizarNivel(@PathVariable Long id, @Valid @RequestBody NivelRequest request) {
        return service.actualizarNivel(id, request);
    }

    @DeleteMapping("/niveles/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarNivel(@PathVariable Long id) {
        service.eliminarNivel(id);
    }

    // ---------- Grados ----------

    @RequierePerfil({Perfil.ADMINISTRADOR, Perfil.DOCENTE, Perfil.ALUMNO})
    @GetMapping("/grados")
    public List<GradoResponse> listarGrados(@RequestParam(required = false) Long nivelId) {
        return service.listarGrados(nivelId);
    }

    @PostMapping("/grados")
    @ResponseStatus(HttpStatus.CREATED)
    public GradoResponse crearGrado(@Valid @RequestBody GradoRequest request) {
        return service.crearGrado(request);
    }

    @PutMapping("/grados/{id}")
    public GradoResponse actualizarGrado(@PathVariable Long id, @Valid @RequestBody GradoRequest request) {
        return service.actualizarGrado(id, request);
    }

    @DeleteMapping("/grados/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarGrado(@PathVariable Long id) {
        service.eliminarGrado(id);
    }

    // ---------- Secciones ----------

    @Operation(summary = "Secciones, opcionalmente filtradas por nivel (Primaria) y grado (3)")
    @RequierePerfil({Perfil.ADMINISTRADOR, Perfil.DOCENTE, Perfil.ALUMNO})
    @GetMapping("/secciones")
    public List<SeccionResponse> listarSecciones(@RequestParam(required = false) String nivel, @RequestParam(required = false) String grado) {
        return service.listarSecciones(nivel, grado);
    }

    @RequierePerfil({Perfil.ADMINISTRADOR, Perfil.DOCENTE})
    @GetMapping("/secciones/{id}")
    public SeccionResponse obtenerSeccion(@PathVariable Long id) {
        return service.obtenerSeccion(id);
    }

    @Operation(summary = "Estudiantes matriculados en la sección")
    @RequierePerfil({Perfil.ADMINISTRADOR, Perfil.DOCENTE})
    @GetMapping("/secciones/{id}/estudiantes")
    public List<EstudianteResponse> estudiantesDeSeccion(@PathVariable Long id) {
        return matriculaService.estudiantesDeSeccion(id);
    }

    @PostMapping("/secciones")
    @ResponseStatus(HttpStatus.CREATED)
    public SeccionResponse crearSeccion(@Valid @RequestBody SeccionRequest request) {
        return service.crearSeccion(request);
    }

    @PutMapping("/secciones/{id}")
    public SeccionResponse actualizarSeccion(@PathVariable Long id, @Valid @RequestBody SeccionRequest request) {
        return service.actualizarSeccion(id, request);
    }

    @DeleteMapping("/secciones/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarSeccion(@PathVariable Long id) {
        service.eliminarSeccion(id);
    }
}
