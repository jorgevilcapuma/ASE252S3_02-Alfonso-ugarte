package pe.edu.vallegrande.app.rest;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import pe.edu.vallegrande.app.dto.request.AvisoFamiliaRequest;
import pe.edu.vallegrande.app.dto.response.AlertaResponses.AlertasResponse;
import pe.edu.vallegrande.app.dto.response.AlertaResponses.AvisoResponse;
import pe.edu.vallegrande.app.dto.response.AlertaResponses.FaltasDia;
import pe.edu.vallegrande.app.dto.response.DashboardResponse;
import pe.edu.vallegrande.app.model.enums.Perfil;
import pe.edu.vallegrande.app.seguridad.RequierePerfil;
import pe.edu.vallegrande.app.seguridad.SesionUsuario;
import pe.edu.vallegrande.app.service.AlertaService;
import pe.edu.vallegrande.app.service.DashboardService;

@Tag(name = "Alertas tempranas y dashboard")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@RequierePerfil({Perfil.ADMINISTRADOR, Perfil.DOCENTE})
public class AlertaRest {

    private final AlertaService service;
    private final DashboardService dashboardService;

    @Operation(summary = "Estudiantes con faltas acumuladas en la ventana (crítica/preventiva) o con falta hoy")
    @GetMapping("/alertas")
    public AlertasResponse alertas() {
        return service.alertas();
    }

    @Operation(summary = "Faltas por día en los últimos N días hábiles (por defecto 7)")
    @GetMapping("/alertas/tendencia")
    public List<FaltasDia> tendencia(@RequestParam(defaultValue = "7") int dias) {
        return service.tendencia(dias);
    }

    @Operation(summary = "Registrar el aviso enviado a las familias")
    @PostMapping("/alertas/avisos")
    @ResponseStatus(HttpStatus.CREATED)
    public List<AvisoResponse> avisar(@Valid @RequestBody AvisoFamiliaRequest request, SesionUsuario sesion) {
        return service.avisarFamilias(request, sesion);
    }

    @Operation(summary = "Resumen institucional del día")
    @GetMapping("/dashboard")
    public DashboardResponse dashboard(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return dashboardService.resumen(fecha);
    }
}
