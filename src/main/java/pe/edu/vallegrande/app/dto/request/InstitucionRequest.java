package pe.edu.vallegrande.app.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record InstitucionRequest(
        @NotBlank(message = "Ingresa el nombre completo de la institución.")
        @Size(min = 5, max = 150, message = "Ingresa el nombre completo de la institución.") String nombre,
        @NotNull(message = "El código modular tiene 7 dígitos.")
        @Pattern(regexp = "\\d{7}", message = "El código modular tiene 7 dígitos.") String codigoModular,
        @NotBlank(message = "Ingresa la dirección de la institución.")
        @Size(min = 5, max = 200, message = "Ingresa la dirección de la institución.") String direccion,
        @NotNull(message = "Usa colores hexadecimales de 6 dígitos, por ejemplo #9b1b1f.")
        @Pattern(regexp = "#[0-9a-fA-F]{6}", message = "Usa colores hexadecimales de 6 dígitos, por ejemplo #9b1b1f.") String colorPrincipal,
        @NotNull(message = "Usa colores hexadecimales de 6 dígitos, por ejemplo #9b1b1f.")
        @Pattern(regexp = "#[0-9a-fA-F]{6}", message = "Usa colores hexadecimales de 6 dígitos, por ejemplo #9b1b1f.") String colorSecundario,
        /* data URL de una imagen PNG/JPG/WEBP de hasta ~2 MB, o vacío para usar el escudo por defecto. */
        @Size(max = 3_000_000, message = "El logotipo supera los 2 MB.")
        @Pattern(regexp = "^$|^data:image/(png|jpeg|webp);base64,[A-Za-z0-9+/=]+$", message = "El logotipo debe ser una imagen PNG, JPG o WEBP.") String logo) {
}
