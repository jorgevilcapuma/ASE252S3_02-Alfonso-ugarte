package pe.edu.vallegrande.app.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Datos del personal para crear o editar (el estado solo cambia con eliminar/restaurar). */
public record PersonalRequest(
        @NotBlank(message = "Ingresa los nombres.") @Size(max = 80, message = "Los nombres admiten 80 caracteres.") String nombres,
        @NotBlank(message = "Ingresa los apellidos.") @Size(max = 80, message = "Los apellidos admiten 80 caracteres.") String apellidos,
        @NotNull(message = "El DNI debe tener exactamente 8 dígitos.")
        @Pattern(regexp = "\\d{8}", message = "El DNI debe tener exactamente 8 dígitos.") String dni,
        @NotBlank(message = "Selecciona el cargo (Docente, Auxiliar, Administrativo, Directivo o Servicio).")
        @Pattern(regexp = "(?i)docente|auxiliar|administrativo|directivo|servicio",
                message = "Selecciona el cargo (Docente, Auxiliar, Administrativo, Directivo o Servicio).") String cargo,
        @Size(max = 80, message = "El área admite 80 caracteres.") String area,
        @Pattern(regexp = "^$|^9\\d{8}$", message = "El celular debe tener 9 dígitos y empezar con 9.") String celular,
        @Email(message = "Ingresa un correo válido.") @Size(max = 120, message = "El correo admite 120 caracteres.") String correo,
        @NotNull(message = "Indica el sexo (M o F).") @Pattern(regexp = "[MFmf]", message = "Indica el sexo (M o F).") String sexo,
        @PastOrPresent(message = "La fecha de ingreso no puede ser futura.") LocalDate fechaIngreso) {
}
