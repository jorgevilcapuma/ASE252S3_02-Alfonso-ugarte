package pe.edu.vallegrande.app.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DocenteRequest(
        @NotBlank(message = "Ingresa los nombres del docente.") @Size(max = 80, message = "Los nombres admiten 80 caracteres.") String nombres,
        @NotBlank(message = "Ingresa los apellidos del docente.") @Size(max = 80, message = "Los apellidos admiten 80 caracteres.") String apellidos,
        @NotNull(message = "El DNI debe tener exactamente 8 dígitos.")
        @Pattern(regexp = "\\d{8}", message = "El DNI debe tener exactamente 8 dígitos.") String dni,
        @Pattern(regexp = "^$|^[0-9 +()-]{6,20}$", message = "Ingresa un teléfono válido.") String telefono,
        @Size(max = 120, message = "La especialidad admite 120 caracteres.") String especialidad,
        @Size(max = 40, message = "La colegiatura admite 40 caracteres.") String colegiatura,
        @Size(max = 30, message = "El código de plaza admite 30 caracteres.") String codigoPlaza,
        @Size(max = 30, message = "La condición admite 30 caracteres.") String condicion,
        @Size(max = 30, message = "El nivel educativo admite 30 caracteres.") String nivelEducativo,
        @PastOrPresent(message = "La fecha de ingreso no puede ser futura.") LocalDate fechaIngreso,
        Long usuarioId) {
}
