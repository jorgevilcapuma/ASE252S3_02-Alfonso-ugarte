package pe.edu.vallegrande.app.rest;

import java.util.List;

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
import pe.edu.vallegrande.app.dto.request.MatriculaRequest;
import pe.edu.vallegrande.app.dto.request.TrasladoRequest;
import pe.edu.vallegrande.app.dto.response.EstudianteResponse;
import pe.edu.vallegrande.app.dto.response.MensajeResponse;
import pe.edu.vallegrande.app.dto.response.PaginaResponse;
import pe.edu.vallegrande.app.model.enums.EstadoMatricula;
import pe.edu.vallegrande.app.model.enums.Perfil;
import pe.edu.vallegrande.app.seguridad.RequierePerfil;
import pe.edu.vallegrande.app.service.MatriculaService;
import pe.edu.vallegrande.app.service.MatriculaService.NivelResumen;

/** Módulo Matrículas del administrador (los identificadores de ruta son de la matrícula). */
@Tag(name = "Matrículas")
@RestController
@RequestMapping("/api/matriculas")
@RequiredArgsConstructor
@RequierePerfil(Perfil.ADMINISTRADOR)
public class MatriculaRest {

    private final MatriculaService service;

    @Operation(summary = "Estudiantes matriculados del año con filtros y paginación (estado por defecto: Activa)")
    @RequierePerfil({Perfil.ADMINISTRADOR, Perfil.DOCENTE})
    @GetMapping
    public PaginaResponse<EstudianteResponse> listar(@RequestParam(required = false) String nivel,
                                                     @RequestParam(required = false) String grado,
                                                     @RequestParam(required = false) String seccion,
                                                     @RequestParam(required = false) Long seccionId,
                                                     @RequestParam(required = false) String q,
                                                     @RequestParam(defaultValue = "Activa") EstadoMatricula estado,
                                                     @RequestParam(defaultValue = "1") int pagina,
                                                     @RequestParam(defaultValue = "20") int tamanio) {
        return service.listar(nivel, grado, seccion, seccionId, q, estado, pagina, tamanio);
    }

    @RequierePerfil({Perfil.ADMINISTRADOR, Perfil.DOCENTE})
    @GetMapping("/resumen")
    public List<NivelResumen> resumen() {
        return service.resumen();
    }

    @GetMapping("/{id}")
    public EstudianteResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @Operation(summary = "Matricular (crea al estudiante si su DNI no existe)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EstudianteResponse matricular(@Valid @RequestBody MatriculaRequest request) {
        return service.matricular(request);
    }

    @Operation(summary = "Trasladar de aula conservando el historial")
    @PutMapping("/{id}/traslado")
    public EstudianteResponse trasladar(@PathVariable Long id, @RequestBody TrasladoRequest request) {
        return service.trasladar(id, request);
    }

    @PatchMapping("/{id}/retiro")
    public EstudianteResponse retirar(@PathVariable Long id) {
        return service.retirar(id);
    }

    @PatchMapping("/{id}/reincorporacion")
    public EstudianteResponse reincorporar(@PathVariable Long id) {
        return service.reincorporar(id);
    }

    @Operation(summary = "Anular: elimina si no hay historial; si lo hay, la retira para conservarlo")
    @DeleteMapping("/{id}")
    public MensajeResponse anular(@PathVariable Long id) {
        return service.anular(id);
    }
}
