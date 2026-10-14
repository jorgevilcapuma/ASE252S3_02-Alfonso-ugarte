package pe.edu.vallegrande.app.rest;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import pe.edu.vallegrande.app.dto.request.InstitucionRequest;
import pe.edu.vallegrande.app.dto.response.InstitucionResponse;
import pe.edu.vallegrande.app.model.enums.Perfil;
import pe.edu.vallegrande.app.seguridad.Publico;
import pe.edu.vallegrande.app.seguridad.RequierePerfil;
import pe.edu.vallegrande.app.service.InstitucionService;

@Tag(name = "Configuración institucional")
@RestController
@RequestMapping("/api/institucion")
@RequiredArgsConstructor
public class InstitucionRest {

    private final InstitucionService service;

    @Operation(summary = "Datos e identidad visual (público: lo usa la pantalla de ingreso)")
    @Publico
    @GetMapping
    public InstitucionResponse obtener() {
        return service.obtener();
    }

    @Operation(summary = "Guardar datos del colegio, colores y logotipo")
    @RequierePerfil(Perfil.ADMINISTRADOR)
    @PutMapping
    public InstitucionResponse actualizar(@Valid @RequestBody InstitucionRequest request) {
        return service.actualizar(request);
    }

    @Operation(summary = "Restablecer colores y logotipo por defecto")
    @RequierePerfil(Perfil.ADMINISTRADOR)
    @PostMapping("/restablecer-identidad")
    public InstitucionResponse restablecerIdentidad() {
        return service.restablecerIdentidad();
    }
}
