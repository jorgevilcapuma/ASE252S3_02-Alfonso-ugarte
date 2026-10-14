package pe.edu.vallegrande.app.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

import pe.edu.vallegrande.app.model.Personal;

public record PersonalResponse(
        Long id,
        String nombres,
        String apellidos,
        String nombreCompleto,
        String dni,
        String cargo,
        String area,
        String celular,
        String correo,
        String sexo,
        LocalDate fechaIngreso,
        String estado,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime deletedAt,
        LocalDateTime restoredAt) {

    public static PersonalResponse de(Personal p) {
        return new PersonalResponse(p.getId(), p.getNombres(), p.getApellidos(), p.getApellidos() + ", " + p.getNombres(), p.getDni(),
                p.getCargo(), p.getArea(), p.getCelular(), p.getCorreo(), p.getSexo(), p.getFechaIngreso(), p.getEstado(),
                p.getCreatedAt(), p.getUpdatedAt(), p.getDeletedAt(), p.getRestoredAt());
    }
}
