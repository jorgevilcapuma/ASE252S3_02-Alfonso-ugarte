package pe.edu.vallegrande.app.rest;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import pe.edu.vallegrande.app.dto.request.PersonalRequest;
import pe.edu.vallegrande.app.dto.response.PersonalResponse;
import pe.edu.vallegrande.app.model.enums.Perfil;
import pe.edu.vallegrande.app.seguridad.RequierePerfil;
import pe.edu.vallegrande.app.service.PersonalService;

/** Tabla maestra del personal: listar, listar por estado, por id, crear, editar, eliminar y restaurar (lógicos). */
@Tag(name = "Personal (tabla maestra)")
@RestController
@RequestMapping("/api/personal")
@RequiredArgsConstructor
@RequierePerfil(Perfil.ADMINISTRADOR)
public class PersonalRest {

    private final PersonalService service;

    @Operation(summary = "Listar personal")
    @GetMapping
    public List<PersonalResponse> listar() {
        return service.listar();
    }

    @Operation(summary = "Listar personal por estado (A = activos, I = inactivos, T = todos)")
    @GetMapping("/estado/{estado}")
    public List<PersonalResponse> listarPorEstado(@PathVariable String estado) {
        return service.listarPorEstado(estado);
    }

    @Operation(summary = "Listar personal por ID")
    @GetMapping("/{id}")
    public PersonalResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @Operation(summary = "Crear personal")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PersonalResponse crear(@Valid @RequestBody PersonalRequest request) {
        return service.crear(request);
    }

    @Operation(summary = "Editar personal")
    @PutMapping("/{id}")
    public PersonalResponse editar(@PathVariable Long id, @Valid @RequestBody PersonalRequest request) {
        return service.editar(id, request);
    }

    @Operation(summary = "Eliminar lógico personal (estado I)")
    @PatchMapping("/eliminar/{id}")
    public PersonalResponse eliminar(@PathVariable Long id) {
        return service.eliminar(id);
    }

    @Operation(summary = "Restaurar lógico personal (estado A)")
    @PatchMapping("/restaurar/{id}")
    public PersonalResponse restaurar(@PathVariable Long id) {
        return service.restaurar(id);
    }
}
