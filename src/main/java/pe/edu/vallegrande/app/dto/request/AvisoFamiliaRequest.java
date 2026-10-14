package pe.edu.vallegrande.app.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;

public record AvisoFamiliaRequest(
        @NotEmpty(message = "Selecciona al menos un estudiante para avisar a su familia.") List<Long> estudianteIds) {
}
