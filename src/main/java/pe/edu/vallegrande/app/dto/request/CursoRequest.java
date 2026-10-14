package pe.edu.vallegrande.app.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CursoRequest(
        @NotBlank(message = "Ingresa el nombre del curso.") @Size(max = 120, message = "El nombre del curso admite 120 caracteres.") String nombre,
        @Size(max = 255, message = "La descripción admite 255 caracteres.") String descripcion) {
}
